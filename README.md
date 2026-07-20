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
