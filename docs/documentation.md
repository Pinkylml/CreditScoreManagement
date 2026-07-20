# Credit Score Management System — Technical Documentation

A guide to the architecture, scoring math, configuration, concurrency model, and storage formats of this system.

---

## 1. Architecture: Ports & Adapters (Hexagonal)

The system keeps business logic isolated from infrastructure concerns (files, serialization, notification channels):

```
                  +----------------------------------------------+
                  |              Infrastructure Layer            |
                  |  [XmlUserStore]  [JsonUserStore]  [Observer] |
                  +-----------------------+----------------------+
                                          |
                                          v (Adapts)
                  +-----------------------+----------------------+
                  |                  Ports Layer                 |
                  |     [UserStore SPI]   [NotificationSender]   |
                  +-----------------------+----------------------+
                                          |
                                          v (Calls)
                  +-----------------------+----------------------+
                  |              Core Domain Layer               |
                  |  [User] [CreditHistoryRecord] [Calculations] |
                  +----------------------------------------------+
```

### Core Domain Layer
- **`User`** — Aggregate root. Holds identity info, credit score, risk level, and the full transaction history. Enforces invariants (non-blank SSN, positive credit limit) at construction time.
- **`CreditHistoryRecord`** — Immutable value object for a single transaction (amount, type, due date, settlement date, status).
- **`ScoreConfiguration`** — Immutable snapshot of scoring weights, grace period, and risk thresholds loaded from the properties file.

### Ports Layer
- **`UserStore`** — Persistence SPI (`save`, `findBySsn`, `findAll`, `deleteBySsn`). The domain never touches the file layer directly.
- **`NotificationSender`** — Notification SPI. Decouples alert dispatch from the scoring logic.

### Services & Orchestration
- **`CreditScoreEngine`** — Facade that exposes all use cases: register, add transaction, evaluate profile, edit, delete. Everything goes through here.
- **`WeightedScoreFormula`** — Concrete scoring strategy. Pluggable via the `ScoreFormula` interface.
- **`RiskClassifier`** — Maps a numeric score to LOW / MEDIUM / HIGH using configurable thresholds.

### Infrastructure Layer
- **`AbstractFileUserStore`** — Template Method base class that handles caching, `StampedLock` concurrency, and DTO mapping. Subclasses only implement `readFromFile()` / `writeToFile()`.
- **`XmlUserStore`** — Reads and writes `users.xml` via JAXB.
- **`JsonUserStore`** — Reads and writes `users.json` via Gson.

---

## 2. External Configuration (`credit-settings.properties`)

All scoring math and storage settings are externalized in `src/main/resources/credit-settings.properties`.

| Property Key | Type | Default | Description |
|---|---|---|---|
| `storage.type` | String | `JSON` | Active storage format. Valid values: `JSON`, `XML`. |
| `weight.credit.utilization` | int | `30` | Max points for credit utilization. |
| `weight.payment.history` | int | `35` | Max points for on-time payment history. |
| `weight.credit.age` | int | `15` | Max points for average account age. |
| `weight.credit.types` | int | `10` | Max points for credit type diversity. |
| `weight.recent.inquiries` | int | `10` | Max penalty for recent hard inquiries (absolute value). |
| `risk.threshold.low` | double | `75.0` | Score at or above this → LOW risk. |
| `risk.threshold.medium` | double | `50.0` | Score at or above this but below low → MEDIUM risk. Below this → HIGH risk. |

---

## 3. Dynamic Persistence Loader

`PropertyWeightLoader.loadStorageType()` reads `storage.type` at startup and passes it to `PersistenceRegistry.getStore()`, which returns the correct adapter:

```java
public static UserStore getStore(StorageType type) {
    switch (type) {
        case XML:  return new XmlUserStore();
        case JSON: return new JsonUserStore();
        default:   throw new IllegalArgumentException("Unknown storage type: " + type);
    }
}
```

---

## 4. Scoring Formulas

`WeightedScoreFormula` evaluates five factors. All results are clamped to `[0.0, 100.0]` before being stored.

1. **Credit Utilization** (max 30 pts): `(totalUsed / totalLimit) * 30`, capped at 30. Zero usage earns a 6-point baseline (20% of max).
2. **Payment History** (max 35 pts): `(onTimePayments / totalPayments) * 35`. A payment is on time only if it was settled on or before the due date. Any settlement after the due date counts as late. DEFAULTED records always count as missed.
3. **Credit Age** (max 15 pts): `(averageAgeInYears / 10) * 15`, capped at 15. Age is measured from each record's due date to today.
4. **Credit Type Diversity** (max 10 pts): `uniqueTypes * 2.0`, capped at 10.
5. **Recent Inquiries** (max −10 pts): `recentCount * −2.0`, floored at −10. Only transactions with a due date in the last 24 months are counted.

---

## 5. Concurrency & Thread Safety

- **`StampedLock` on `AbstractFileUserStore`**: Reads first try an optimistic read; if a concurrent write is detected during validation, they fall back to a blocking read lock. Writes always use an exclusive lock and flush to disk immediately.
- **Defensive cloning on `CreditHistoryRecord`**: `dueDate` and `settlementDate` are cloned on construction and in their getters to prevent external mutation of internal state.
- **`Collections.unmodifiableList`**: `User.getCreditHistory()` returns a read-only view.
- **`ConcurrentHashMap`**: The in-memory cache in `AbstractFileUserStore`.
- **`CopyOnWriteArrayList`**: The listener list in `CreditEventPublisher` allows safe subscription changes during notification dispatch.
- **Async notifications via `CompletableFuture.runAsync()`**: Email/SMS delivery runs on a separate thread so it never blocks the scoring pipeline.

---

## 6. Notification Events

A notification is sent when `evaluateProfile()` detects one of the following:
- The user's **risk level changed** (e.g., MEDIUM → LOW).
- The **score delta is ≥ 10 points** (regardless of direction).

```java
if (previousRisk != newRisk) {
    String alert = String.format("Your credit risk level shifted from %s to %s.", previousRisk, newRisk);
    notificationSender.sendNotification(user, alert);
} else if (Math.abs(previousScore - newScore) >= 10.0) {
    String alert = String.format("Significant shift detected. New score is %.2f.", newScore);
    notificationSender.sendNotification(user, alert);
}
```

---

## 7. Storage File Formats

### XML (`users.xml`)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<creditSystem>
    <users>
        <user>
            <ssn>111-22-3333</ssn>
            <name>Jefferson Cando</name>
            <address>Quito, Ecuador</address>
            <creditScore>52.5</creditScore>
            <riskLevel>MEDIUM</riskLevel>
            <totalCreditLimit>10000.0</totalCreditLimit>
            <creditHistory>
                <record>
                    <transactionId>721f216d-5b02-4dfa-84c7-0ca24c5296e5</transactionId>
                    <dueDate>2025-07-13</dueDate>
                    <settlementDate>2025-07-18</settlementDate>
                    <type>AUTO_LOAN</type>
                    <amount>450.0</amount>
                    <status>PAID</status>
                </record>
            </creditHistory>
            <email>jeff@example.com</email>
        </user>
    </users>
</creditSystem>
```

### JSON (`users.json`)

```json
[
  {
    "ssn": "111-22-3333",
    "name": "Jefferson Cando",
    "address": "Quito, Ecuador",
    "email": "jeff@example.com",
    "creditScore": 52.5,
    "totalCreditLimit": 10000.0,
    "riskLevel": "MEDIUM",
    "creditHistory": [
      {
        "transactionId": "721f216d-5b02-4dfa-84c7-0ca24c5296e5",
        "dueDate": "2025-07-13",
        "settlementDate": "2025-07-18",
        "transactionType": "AUTO_LOAN",
        "amount": 450.0,
        "status": "PAID"
      }
    ]
  }
]
```

---

## 8. Build & Run

```bash
# Run unit tests
./gradlew test

# Run the five demo scenarios
./gradlew run
```
