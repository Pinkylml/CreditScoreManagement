# 04. Infrastructure & Dependencies

This document details the critical infrastructure mechanics, third-party library integrations, and file-system safeguards employed by the **CreditScoreManagement** system to ensure data integrity and lossless persistence.

---

## 1. Third-Party Dependencies

The infrastructure layer relies on exactly two external serialization engines, abstracted behind the `UserStore` port.
*   **Gson (2.10.1)**: Driven by `JsonUserStore`, this engine rapidly handles deep object graph serialization into standard JSON text representations.
*   **JAXB (Java Architecture for XML Binding)**: Driven by `XmlUserStore`, this built-in Java 8 engine handles strict XML marshalling using declarative DTO annotations (`@XmlRootElement`, `@XmlElement`).

---

## 2. Lossless Serialization (Data Transfer Objects)

To keep the core domain mathematically precise while satisfying the rigid property constraints of JAXB and Gson, the system implements a strict Data Transfer Object (DTO) boundary.

### Precision Preservation
The domain aggregate (`User`) and its value objects utilize `java.math.BigDecimal` for all monetary variables (`amount`, `totalCreditLimit`) to avoid floating-point binary loss. 
However, standard serializers can implicitly downcast numbers or enforce locale-specific scientific notation during serialization. To guarantee 100% precision retention across the file system boundary:
1.  **Write Path**: During the mapping phase (`AbstractFileUserStore.mapToDto`), all `BigDecimal` values are safely converted to explicit character sequences via `toPlainString()`. The resulting DTO properties (e.g., `UserStorageDto.totalCreditLimitStr`) are strongly typed as `String`.
2.  **Read Path**: When loading from disk, the string literals are passed directly into the `new BigDecimal(String)` constructor during `mapToDomain`, seamlessly restoring the exact unrounded financial state.

---

## 3. Atomic Write Resilience (Temp-and-Swap)

A primary directive of the persistence layer is to prevent catastrophic data corruption. If the JVM crashes, the host machine loses power, or disk quota is exhausted during a file write, partially written XML or JSON structures would render the entire database unreadable upon reboot.

To mitigate this, `XmlUserStore` and `JsonUserStore` implement a strict **Temp-and-Swap** atomic write workflow:

1.  **Temporary Write Pipeline**: Instead of overwriting the live database file directly, the serializer outputs the structured data into a secondary temporary file (e.g., `users.xml.tmp`).
2.  **Stream Finalization**: The underlying file streams and writers are strictly flushed and closed. This guarantees the OS has pushed the data to the temporary disk node.
3.  **Atomic Swap Operation**: The system invokes `java.nio.file.Files.move()` using the `StandardCopyOption.ATOMIC_MOVE` parameter. This instructs the host operating system's file controller to atomically rename the `.tmp` file over the live target file.
4.  **Fallback Strategy**: In environments where native OS atomic moves are not supported across the specific volume layout, the system catches the `AtomicMoveNotSupportedException` and immediately degrades to `StandardCopyOption.REPLACE_EXISTING`.

If any step in this sequence fails, a custom `PersistenceException` is thrown, aborting the swap and leaving the original, intact database file untouched.
