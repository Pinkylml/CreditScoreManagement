# 03. Domain Model & Boundaries

This document defines the core aggregates, value objects, domain rules, and exception contracts that constitute the pure business logic layer of the **CreditScoreManagement** system.

---

## 1. Domain Entities & Value Objects

The Domain layer operates with zero external dependencies, enforcing strict business invariants through encapsulation and immutability.

### A. Aggregate Root: `User`
The `User` entity acts as the transactional boundary and aggregate root for a client's credit profile.
*   **Encapsulation**: State mutation is strictly controlled via domain methods (e.g., `addCreditRecord`, `updateProfile`). Direct property access is blocked.
*   **Defensive Copying**: To prevent reference leaks across thread boundaries, `User` implements a defensive copy constructor (`new User(User source)`) that deep-copies all internal fields and collections.
*   **Financial Precision**: All monetary metrics, such as `totalCreditLimit`, are typed strictly as `java.math.BigDecimal`. This entirely prevents the catastrophic binary floating-point rounding errors inherent to `double` primitives in financial calculations.

### B. Value Object: `CreditHistoryRecord`
Represents an immutable ledger entry of a specific financial transaction.
*   **Immutability**: Once instantiated, the record cannot be altered. Edits must be enacted by the aggregate root replacing the entire object.
*   **Temporal Tracking**: Tracks both the expected `dueDate` and the actual `settlementDate` to allow the calculation engine to accurately determine payment punctuality.

---

## 2. Strict Business Rules & Invariants

The `WeightedScoreFormula` enforces strict semantic isolation between different classes of credit transactions based on their `TransactionType`.

### Inquiry Isolation Semantics
Hard credit inquiries (`TransactionType.INQUIRY`) represent external checks against a profile without conferring an actual credit line.
*   **Exclusion**: Inquiry records are strictly filtered out of calculations for Credit Age, Credit Utilization, and Payment History. Including them would erroneously skew the average account age or imply a 100% on-time payment rate for a non-payable event.
*   **Penalty Application**: Inquiries are exclusively processed by the `computeInquiryPoints` algorithm. The formula scans for any inquiries occurring strictly within the last 24 months, applying a flat point deduction for each occurrence.

---

## 3. Domain Exception Hierarchy

To guarantee robust error handling and clear API contracts, the system eschews generic Java exceptions in favor of a specialized domain exception hierarchy located in `com.montran.creditscore.domain.exception`.

*   **`DuplicateUserException`**: Thrown by the core engine when attempting to register a `User` whose Social Security Number (SSN) already exists in the `UserStore`. Enforces identity uniqueness.
*   **`CreditCalculationException`**: Thrown by the scoring algorithms (e.g., `WeightedScoreFormula`) when required domain data is missing or corrupted. For example, attempting to calculate utilization points on a profile with a `null` or `0` credit limit triggers this exception to halt calculation and prevent divide-by-zero math faults.
*   **`PersistenceException`**: Thrown by the infrastructure adapters and caught/propagated by the ports. It wraps all underlying I/O, serialization, or filesystem faults (such as a locked `.tmp` file or an XML marshalling failure) into a uniform, domain-understood failure state without leaking underlying library footprints.
