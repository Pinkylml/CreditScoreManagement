# 02. Creational Design Patterns

This document details the creational design patterns implemented in the **CreditScoreManagement** system. These patterns isolate class instantiation logic, ensuring the client application is decoupled from concrete configurations and dependencies.

---

## 1. Factory Pattern / Registry

### Description & Intent
The system uses a centralized creational factory, **`PersistenceRegistry`**, to instantiate and supply the appropriate outbound persistence adapter at runtime based on the requested configuration. This prevents client code from directly instantiating the concrete classes (`XmlUserStore` or `JsonUserStore`) and couples them only to the abstract `UserStore` interface (an Outbound Port in Hexagonal Architecture).

### Implementation Details

#### The `StorageType` Enum
The application uses an enumeration to represent the available physical storage engines:
```java
public enum StorageType {
    XML,
    JSON
}
```
This value is loaded at startup from `credit-settings.properties` via the `PropertyWeightLoader`.

#### The `PersistenceRegistry` Class
The registry enforces strict factory initialization:
1. Direct instantiation is blocked by a `private` constructor throwing an `UnsupportedOperationException`.
2. The static factory method `getStore(StorageType)` processes the provided enum option and returns the fully initialized store.
3. Explicit `null` checks protect the factory from resolving invalid runtime configurations.

```java
public final class PersistenceRegistry {
    private PersistenceRegistry() {
        throw new UnsupportedOperationException("Utility factory class cannot be instantiated.");
    }

    public static UserStore getStore(StorageType type) {
        if (type == null) {
            throw new IllegalArgumentException("Storage type configuration cannot be null.");
        }
        switch (type) {
            case XML:
                return new XmlUserStore();
            case JSON:
                return new JsonUserStore();
            default:
                throw new IllegalArgumentException("Unsupported storage framework requested: " + type);
        }
    }
}
```

### Architectural Benefits

1. **Low Coupling**: The core orchestrators and client application (`Main.java`) do not need to import `XmlUserStore` or `JsonUserStore`. They operate strictly against the `UserStore` abstraction.
2. **Encapsulated Initialization**: Any complexity related to file setups, framework contexts (e.g. `JAXBContext`), or caching maps is entirely hidden behind the factory method boundary.
3. **Single Configuration Origin**: Adding a new persistence mechanism (such as a database adapter) requires adding a new `StorageType` enum and one switch case in this registry—respecting isolation and preserving maintainability.
