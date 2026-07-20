# 02. Behavioral Design Patterns

This document describes the behavioral design patterns implemented in the **CreditScoreManagement** system. These patterns govern how responsibilities, control flows, and communications are managed between objects.

---

## 1. Template Method Pattern

### Description & Intent
The Template Method pattern defines the skeleton of an algorithm in a base class, deferring some implementation steps to subclasses. This enforces a standardized workflow while allowing concrete variations.

In this system, **`AbstractFileUserStore`** acts as the template for persistence. It standardizes the processes of concurrent lock management (`StampedLock`), domain-to-DTO data mapping, and cache coordination. The physical act of disk I/O and format serialization is delegated to its concrete subclasses.

### The `save(User)` Workflow
When persisting a user, `AbstractFileUserStore` enforces a rigid algorithmic template:
1. **Lock**: Acquire an exclusive write lock to block concurrent cache access.
2. **Cache Update**: Insert or overwrite the user aggregate in the `ConcurrentHashMap` cache.
3. **Map & Delegate**: Transform the entire cache into flat DTOs (`UserStorageDto`) and invoke the abstract hook method `writeToFile(List<UserStorageDto>)`.
4. **Unlock**: Release the lock safely in a `finally` block.

Concrete subclasses (`XmlUserStore` and `JsonUserStore`) implement `writeToFile` to execute the format-specific parsing and atomic temp-and-swap file I/O.

---

## 2. Strategy Pattern

### Description & Intent
The Strategy Pattern defines a family of algorithms, encapsulates each one, and makes them interchangeable, allowing the algorithm to vary independently from the clients that use it.

In the credit scoring engine, the mathematical logic required to evaluate a credit profile is abstracted behind the **`ScoreFormula`** strategy interface.

### Implementation
The `CreditScoreEngine` orchestrator does not contain hardcoded scoring math. Instead, it delegates to the configured `ScoreFormula`. 
The primary concrete implementation, **`WeightedScoreFormula`**, calculates a final rating by assessing distinct criteria, isolating the complexity:
1. **Credit Utilization Ratio**: Evaluates credit usage against the total credit limit.
2. **Payment History**: Computes payment punctuality based on elapsed settlement dates.
3. **Credit Age**: Evaluates the longevity of the user's accounts.
4. **Credit Types**: Awards points for diversity in credit instruments (e.g., mortgages vs. credit cards).
5. **Inquiries Penalty**: Deducts points for hard inquiries (`TransactionType.INQUIRY`) logged within the last 24 months.

---

## 3. Observer Pattern

### Description & Intent
The Observer Pattern defines a one-to-many dependency so that when a subject changes state, all registered dependents (observers) are notified and updated automatically.

This pattern is used to decouple the core scoring logic from external alerting mechanisms, such as sending emails or SMS messages when a user's risk level shifts.

### Implementation
*   **Subject Interface**: The `NotificationSender` outbound port.
*   **Subject Implementation**: `CreditEventPublisher` manages the subscription list and broadcasts the events.
*   **Observer Interface**: `CreditEventListener` defines the `onCreditEvent(User, message)` contract.
*   **Concrete Observers**: `EmailNotificationListener` and `SmsNotificationListener` act as the terminal delivery endpoints.

### Concurrency Safety in Observers
1. **Thread-Safe Registration**: `CreditEventPublisher` uses a `CopyOnWriteArrayList` to store its listeners. This allows observers to be safely added or removed dynamically at runtime without throwing `ConcurrentModificationException` during an active broadcast.
2. **Asynchronous Dispatch**: Alerting gateways (like SMTP servers or SMS APIs) are often slow. To prevent network latency from blocking the main `CreditScoreEngine` thread, the publisher dispatches every notification asynchronously via `CompletableFuture.runAsync()`.
