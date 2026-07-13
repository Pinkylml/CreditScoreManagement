# Credit Score Management System

A robust, enterprise-grade **Credit Score Management System** built using **Hexagonal Architecture (Ports & Adapters)** and pure **Java 8**. The system evaluates client credit profiles, tracks transactional histories, calculates credit ratings dynamically based on external settings, and broadcasts async events on significant shifts.

---

## 1. Architectural Highlights

* **Hexagonal Architecture (Ports & Adapters)**: Strict inward-pointing dependencies. Core domain logic has zero dependencies on external libraries (like JAXB/Gson), databases, or network protocols.
* **Optimistic Reader-Writer Locks (`StampedLock`)**: Enforces concurrency safety across transactional collections. Optimistic reads are checked for validation, falling back to strict read locks only when a write interference is detected, maximizing thread throughput.
* **Strategy Pattern**: Score formulas are encapsulated into interchangeable algorithms ([ScoreFormula](docs/03_domain_model.md#class-scoreformula)), allowing administrators to plug in new evaluation scoring systems dynamically.
* **Observer Pattern**: Broadcasts score and risk classification adjustments asynchronously using `CompletableFuture.runAsync()` to decoupled subscribers ([CreditEventListener](docs/02_design_patterns/behavioral_patterns.md#9-behavioral-pattern-observer-event-notification-diagram)).
* **Template Method Pattern**: The base file store ([AbstractFileUserStore](docs/02_design_patterns/creational_patterns.md#4-creational-pattern-registry-factory-diagram)) implements memory-caching, transaction locks, and mapping flows, leaving formatting serialization (JAXB XML or Gson JSON) to concrete adapters.

---

## 2. Dynamic Configurations

All math weights, storage mechanisms, and risk limits are externalized within **[credit-settings.properties](src/main/resources/credit-settings.properties)**:

| Key | Type | Default | Purpose |
|---|---|---|---|
| `storage.type` | String | `JSON` | Active database format adapter (`JSON` or `XML`). |
| `weight.credit.utilization` | Integer | `30` | Max point allocation for utilization ratio. |
| `weight.payment.history` | Integer | `35` | Max point allocation for timely payments. |
| `weight.credit.age` | Integer | `15` | Max point allocation for account age. |
| `weight.credit.types` | Integer | `10` | Max point allocation for category diversity. |
| `weight.recent.inquiries` | Integer | `10` | Max point allocation penalty for recent inquiries. |
| `late.payment.grace.days` | Integer | `30` | Grace period in days before a payment is penalized. |
| `risk.threshold.low` | Double | `75.0` | Credit score threshold above which risk is LOW. |
| `risk.threshold.medium` | Double | `50.0` | Credit score threshold below which risk is HIGH. |

---

## 3. Documentation Roadmap

* **[01. System Architecture Overview](docs/01_architecture_overview.md)**: Deep-dive into Ports & Adapters layers, stack, and data flows.
* **[02. Creational Design Patterns](docs/02_design_patterns/creational_patterns.md)**: Explains Registry Factory persistence and dynamic instantiation.
* **[03. Structural Design Patterns](docs/02_design_patterns/structural_patterns.md)**: Details Adapter wrappers, DTO mapping boundaries, and Facade orchestrators.
* **[04. Behavioral Design Patterns](docs/02_design_patterns/behavioral_patterns.md)**: Covers Template Method, Strategy, and Observer notifier loops.
* **[05. Domain Model & Boundaries](docs/03_domain_model.md)**: Outlines Aggregate Roots, Value Objects, and Thread-Safety locks.
* **[06. Infrastructure Dependencies](docs/04_infrastructure_deps.md)**: Details settings file variables, caching, and DTO marshallers.

---

## 4. Getting Started

### Prerequisites
* **Java Development Kit (JDK) 8** or higher.
* **Gradle** (executable wrapper `./gradlew` is included).

### Compile and Verify Test Suite
Execute the integration and serialization tests:
```bash
./gradlew test
```

### Run Demonstration Scenarios
Boot the core application to execute the five evaluation scenarios and CRUD transaction showcase:
```bash
./gradlew run
```
