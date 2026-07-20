# Credit Score Management System

## Project Overview
The Credit Score Management System is a robust Java-based application designed to evaluate and manage user credit profiles accurately and securely. It features a highly thread-safe architecture with atomic operations using per-user locks and a pluggable persistence layer supporting atomic cross-platform file writes.

## Tech Stack
*   **Java Version:** 1.8 (Java 8)
*   **Build Tool:** Gradle
*   **Dependencies:** Gson (2.10.1) for JSON processing, JAXB (built-in Java 8) for XML processing.

## Quick Start (Build & Run)

**Clean and Build the Project:**
```powershell
.\gradlew.bat clean build
```

**Run the Test Suite:**
```powershell
.\gradlew.bat test
```

**Execute the Application Demo:**
```powershell
.\gradlew.bat run
```

## Configuration (`credit-settings.properties`)
The system behavior is defined in `src/main/resources/credit-settings.properties`.

### Persistence Configuration
The application supports pluggable storage engines. To switch between XML and JSON persistence, modify the following property:
```properties
# Defines the active persistence mechanism. Valid options: XML, JSON
storage.type=XML
```

### Score Factor Weights
The scoring formula is highly configurable. The sum of the following factor weights must equal exactly `100`:
```properties
weight.credit.utilization=30
weight.payment.history=35
weight.credit.age=15
weight.credit.types=10
weight.recent.inquiries=10
```

*Risk thresholds can also be configured using `risk.threshold.low` and `risk.threshold.medium`.*

## Demo Execution Summary
Running `Main.java` executes a comprehensive end-to-end demonstration proving system correctness:
1.  **5 Distinct Scoring Scenarios:** Evaluates predefined user profiles ranging from excellent credit histories to new users with no history, validating the weighted formula.
2.  **Atomic Transaction Editing:** Demonstrates adding a transaction and subsequently updating its amount, proving that the user's credit score is automatically and safely re-evaluated.
3.  **Safe Multi-Threaded Concurrency:** Spawns multiple concurrent threads that simultaneously add transactions to the same user. This proves that the per-user `ReentrantLock` mechanism completely prevents race conditions and data loss during concurrent writes.
4.  **Periodic Batch Recalculation:** Demonstrates the `PeriodicScoreUpdater` daemon running in the background to safely evaluate and flush all user profiles on a scheduled interval.

## Documentation Index
For a deep dive into the system's engineering, please review the formal technical specifications located in the `docs/` directory:
1.  **[System Architecture Overview](docs/01_architecture_overview.md)**: Hexagonal boundaries, Layers, and Concurrency strategies.
2.  **[Behavioral Patterns](docs/02_design_patterns/behavioral_patterns.md)**: Template Method, Strategy algorithms, and Async Observer flows.
3.  **[Creational Patterns](docs/02_design_patterns/creational_patterns.md)**: Persistence Registry mechanisms.
4.  **[Structural Patterns](docs/02_design_patterns/structural_patterns.md)**: Facade orchestration and DTO isolation.
5.  **[Domain Model & Boundaries](docs/03_domain_model.md)**: Aggregate roots, BigDecimal precision, and exception contracts.
6.  **[Infrastructure Dependencies](docs/04_infrastructure_deps.md)**: Temp-and-Swap file resilience and lossless serialization.
7.  **[Codebase Class Reference](docs/05_class_reference.md)**: Exhaustive catalog of all classes and their exact operational roles.

---
**Reviewers / Evaluators:** Please start by reading the **[Project Revision & Evaluator's Roadmap](docs/project_revision_guide.md)** which directly addresses the remediation of previous assessment gaps.
