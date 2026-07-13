# 04. Infrastructure, Dependencies, and Setup

This document describes the infrastructure configuration, build dependencies, configuration files, and setup instructions for the **CreditScoreManagement** system.

---

## 1. Build Framework: Gradle

The project is managed using Gradle. Below is the detailed breakdown of the dependencies and configuration specified in `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'application'
}

group = 'com.montran.creditscore'
version = '1.0-SNAPSHOT'

sourceCompatibility = 1.8
targetCompatibility = 1.8

repositories {
    mavenCentral()
}

dependencies {
    // Platform BOM to manage JUnit version alignment
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    // Google Gson for JSON parsing and serialization (Java 8 compatible)
    implementation 'com.google.code.gson:gson:2.10.1'
}

test {
    useJUnitPlatform()
}

application {
    mainClassName = 'com.montran.creditscore.Main'
}
```

### Dependency Breakdown:
1. **Java Compatibility**: The project is compiled targeting **Java 8 (v1.8)**. This is a common target in enterprise systems requiring compatibility with legacy runtimes.
2. **JAXB Framework**: Since the project targets Java 8, the **JAXB API** (`javax.xml.bind`) is historically provided natively by the JDK runtime. No external Maven dependencies are declared in `build.gradle` for JAXB.
3. **Google Gson (v2.10.1)**: Used by `JsonUserStore` to serialize and deserialize `SystemContainerDto` documents with formatted printing.
4. **JUnit 5 (JUnit Jupiter)**: Used to write and run unit and integration tests.

---

## 2. Configuration Settings

### Settings Properties File: `src/main/resources/credit-settings.properties`
This standard properties configuration file is used to store runtime environment variables and adjustments. It contains the following optional keys, parsed dynamically during calculation initialization:

| Property Key | Type | Default Value | Description |
|---|---|---|---|
| `weight.credit.utilization` | Integer | `30` | Target weight weight applied to the credit utilization ratio (0-100 scale). |
| `weight.payment.history` | Integer | `35` | Target weight applied to the user's historical payment timeliness. |
| `weight.credit.age` | Integer | `15` | Target weight applied to the average age of open accounts. |
| `weight.credit.types` | Integer | `10` | Target weight applied to the diversity of account categories held. |
| `weight.recent.inquiries` | Integer | `10` | Target weight applied to penalize recent hard credit inquiries. |
| `late.payment.grace.days` | Integer | `30` | Grace period in days before a payment is officially penalized as late. |

### Configuration Parser: `PropertyWeightLoader`
The loader class [PropertyWeightLoader](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/config/PropertyWeightLoader.java) executes properties loading via standard InputStream reads:

1. **Classpath Resolution**: Attempts to locate `credit-settings.properties` in the application classpath.
2. **Safe Fallbacks**: If the file is missing or contains invalid integer formats, it captures exceptions and logs warnings to `System.err`, applying the specified default values.
3. **Domain Mapping**: Outputs a fully populated [ScoreConfiguration](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/ScoreConfiguration.java) object which acts as an immutable Value Object for calculation threads.

---

## 3. Data Schema Layouts

The adapters serialize DTO data structures to the project directory root.

### A. JSON Document Layout (`users.json`)
The JSON structure managed by the Google Gson engine outputs flat collections nested within a root object. Gson maps fields directly to Java variable names:

```json
{
  "users": [
    {
      "ssn": "111-22-3333",
      "name": "Jefferson Cando",
      "address": "Quito, Ecuador",
      "email": "jeff@example.com",
      "creditScore": 85.5,
      "riskLevel": "LOW",
      "totalCreditLimit": 10000.0,
      "creditHistory": [
        {
          "dueDateStr": "2026-07-13",
          "settlementDateStr": "2026-07-14",
          "transactionType": "CREDIT_CARD",
          "amount": 5000.0,
          "status": "PAID"
        }
      ]
    }
  ]
}
```

### B. XML Document Layout (`users.xml`)
The XML structure managed by the JAXB architecture maps DTO classes to custom XML tags according to `@XmlElement` and `@XmlElementWrapper` annotations:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<creditSystem>
    <users>
        <user>
            <ssn>111-22-3333</ssn>
            <name>Jefferson Cando</name>
            <address>Quito, Ecuador</address>
            <email>jeff@example.com</email>
            <creditScore>85.5</creditScore>
            <riskLevel>LOW</riskLevel>
            <totalCreditLimit>10000.0</totalCreditLimit>
            <creditHistory>
                <record>
                    <dueDate>2026-07-13</dueDate>
                    <settlementDate>2026-07-14</settlementDate>
                    <type>CREDIT_CARD</type>
                    <amount>5000.0</amount>
                    <status>PAID</status>
                </record>
            </creditHistory>
        </user>
    </users>
</creditSystem>
```

---

## 4. Setup and Execution Guide

### Prerequisites
* **JDK 8 (1.8)** must be installed and configured as the default SDK (`java -version`).
* Gradle wrapper handles local tool downloading.

### Execution Commands

To compile, build, and test the project, run the following commands in the workspace root directory:

#### Clean and Compile
```powershell
./gradlew clean compileJava
```

#### Run Tests
Runs all unit and integration test suites:
```powershell
./gradlew test
```

#### Execute Application
Runs the entry point class `com.montran.creditscore.Main`:
```powershell
./gradlew run
```

---

## 5. Critical Troubleshooting: JDK 9+ Compilation Issues (JAXB Removal)

> [!WARNING]
> If Gradle uses a Java version higher than 8 (e.g. Java 11, 17, or 21) as its compilation JVM runtime, the build will fail with compilation errors stating `package javax.xml.bind does not exist`. 
>
> This happens because JAXB was deprecated in JDK 9 and completely removed from the standard JDK libraries in JDK 11.

To resolve this issue when building the application on systems where Gradle is run via Java 9+, choose one of the following two solutions:

### Solution A: Force Gradle to Use Java 8 (Recommended)
Configure the project's target JVM directly for Gradle by specifying the path to your Java 8 JDK in the `gradle.properties` file:

1. Create a `gradle.properties` file in the project root (if it does not exist).
2. Add the following line pointing to your local JDK 8 installation directory:
   ```properties
   org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-8.0.492.1-hotspot
   ```

### Solution B: Add JAXB Dependencies to `build.gradle`
If you must build and run the application on JDK 9+ directly, add external JAXB dependencies to the `dependencies` block in [build.gradle](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/build.gradle):

```groovy
dependencies {
    // ... other dependencies ...

    // JAXB API and Runtime for Java 9+ compatibility
    implementation 'javax.xml.bind:jaxb-api:2.3.1'
    implementation 'org.glassfish.jaxb:jaxb-runtime:2.3.1'
}
```
This forces Gradle to pull the external JAXB jars during compile and test execution, regardless of the active JDK version.
