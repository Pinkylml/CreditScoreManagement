# 02. Structural Design Patterns

This document describes the structural design patterns implemented in the **CreditScoreManagement** system. These patterns govern how classes and interfaces compose to form larger, flexible systems while preserving strict concurrency safety and clear boundaries.

---

## 1. Facade Pattern

### Description & Intent
The Facade Pattern provides a unified, simplified interface to a complex subsystem. It hides the underlying structural complexities from the client application, offering a single point of interaction.

In **CreditScoreManagement**, the **`CreditScoreEngine`** acts as the definitive Facade and aggregate orchestrator. Client applications (like `Main.java`) never interact directly with the persistence store, scoring formulas, or locking primitives.

### Orchestration Responsibilities
The `CreditScoreEngine` hides massive complexity behind simple API calls (e.g., `addTransactionToUser`):
1. **Concurrency Orchestration**: It natively manages a `ConcurrentHashMap<String, ReentrantLock>` to guarantee that all operations on a specific user's SSN are strictly atomic. The client is completely unaware of the locking semantics.
2. **Persistence Orchestration**: It retrieves the user via the `UserStore` and explicitly persists the state back to the store after a successful mutation.
3. **Calculation Orchestration**: It coordinates the `PropertyWeightLoader` (to get configuration rules), the `ScoreFormula` (to run the math), and the `RiskClassifier` (to categorize the resulting score).
4. **Notification Orchestration**: It automatically dispatches alerts via the `NotificationSender` if the score drastically changes or the risk category shifts.

---

## 2. Adapter Pattern

### Description & Intent
The Adapter pattern allows incompatible interfaces to work together. In Hexagonal architecture, adapters are the concrete translation layers connecting the system to external physical infrastructure.

Here, **`XmlUserStore`** and **`JsonUserStore`** adapt physical file operations (using external libraries like JAXB and Gson) to conform to the **`UserStore`** interface required by the core logic.

### Implementation
- **XML Adapter (`XmlUserStore`)**: Uses Java's built-in JAXB marshallers to write structural XML, implementing a safe, atomic temp-and-swap mechanism (`users.xml.tmp` → `users.xml`) beneath the `UserStore` contract.
- **JSON Adapter (`JsonUserStore`)**: Uses Google Gson to write serialized JSON, identically hiding the atomic file I/O mechanics from the core application.

---

## 3. Data Transfer Object (DTO) Pattern

### Description & Intent
To prevent serialization annotations, reflection constraints, and storage-specific requirements from polluting the core Domain layer, the system strictly separates models using the Data Transfer Object (DTO) pattern.

The domain entity `User` contains complex invariant checking, deep copy constructors, and private lists. In contrast, the DTO classes (`UserStorageDto`, `CreditHistoryStorageDto`) are flat, mutable, and heavily annotated with `@XmlElement` to appease framework parsers.

The `AbstractFileUserStore` controls the boundary translation between these two shapes, ensuring the domain layer remains perfectly pure.

---

## 4. Multi-Tier Concurrency Architecture

The system's structural integrity under heavy multi-threading is achieved by separating concurrency into two distinct structural tiers:

1. **State Mutation (The Facade Tier)**:
   As mentioned, `CreditScoreEngine` owns a `ReentrantLock` for every registered SSN. This forces all read-modify-write workflows targeting the same aggregate root into strict serialization.
2. **Cache Integrity (The Store Tier)**:
   Beneath the engine, `AbstractFileUserStore` maintains a highly efficient memory cache backed by a `StampedLock`. 
   - **Optimistic Reads**: High-frequency, lock-free lookups validate cache states cleanly.
   - **Defensive Copies**: When a cached aggregate is retrieved, the store invokes the domain's deep-copy constructor, preventing any external thread from mutating the shared cache reference.
   - **Atomic Disk Flushes**: Write operations inside the store hold an exclusive write lock, ensuring the entire cache is safely flushed to disk without intermediate states leaking to other readers.
