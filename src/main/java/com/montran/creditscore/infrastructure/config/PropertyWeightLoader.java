package com.montran.creditscore.infrastructure.config;

import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.infrastructure.persistence.StorageType;

import java.io.InputStream;
import java.util.Properties;

/**
 * Reads {@code credit-settings.properties} from the classpath and builds
 * configuration objects from it. Falls back to hardcoded defaults for any
 * missing or malformed property so the system always starts cleanly.
 */
public class PropertyWeightLoader {

    private static final String PROPERTIES_FILE = "credit-settings.properties";

    private PropertyWeightLoader() {
    }

    /**
     * Loads scoring weights, grace period, and risk thresholds from the properties file.
     * Falls back to specification defaults if the file is missing or a value cannot be parsed.
     *
     * @return A fully populated {@link ScoreConfiguration}.
     */
    public static ScoreConfiguration loadWeights() {
        Properties props = new Properties();

        try (InputStream input = PropertyWeightLoader.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
            } else {
                System.err.println("Warning: " + PROPERTIES_FILE + " not found. Applying system defaults.");
            }
        } catch (Exception e) {
            System.err.println("Error reading " + PROPERTIES_FILE + ". Applying system defaults. Reason: " + e.getMessage());
        }

        int utilization = parseProperty(props, "weight.credit.utilization", 30);
        int paymentHistory = parseProperty(props, "weight.payment.history", 35);
        int creditAge = parseProperty(props, "weight.credit.age", 15);
        int creditTypes = parseProperty(props, "weight.credit.types", 10);
        int recentInquiries = parseProperty(props, "weight.recent.inquiries", 10);
        int graceDays = parseProperty(props, "late.payment.grace.days", 30);
        double lowRisk = parseDoubleProperty(props, "risk.threshold.low", 75.0);
        double mediumRisk = parseDoubleProperty(props, "risk.threshold.medium", 50.0);

        return new ScoreConfiguration(utilization, paymentHistory, creditAge, creditTypes, recentInquiries, graceDays, lowRisk, mediumRisk);
    }

    private static double parseDoubleProperty(Properties props, String key, double defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try { return Double.parseDouble(val.trim()); }
            catch (NumberFormatException e) { System.err.println("Warning: Invalid number format for property " + key); }
        }
        return defaultValue;
    }

    private static int parseProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid numeric format for property '" + key + "'. Falling back to " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Reads the {@code storage.type} property and returns the matching {@link StorageType}.
     * Defaults to XML if the value is missing, blank, or unrecognized.
     */
    public static StorageType loadStorageType() {
        Properties props = new Properties();
        try (InputStream in = PropertyWeightLoader.class.getClassLoader().getResourceAsStream("credit-settings.properties")) {
            if (in != null) {
                props.load(in);
                String typeStr = props.getProperty("storage.type");
                if (typeStr != null && !typeStr.trim().isEmpty()) {
                    return StorageType.valueOf(typeStr.trim().toUpperCase());
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Invalid storage.type configuration detected. Defaulting safely to XML.");
        } catch (Exception e) {
            System.err.println("Warning: Could not read settings file for storage type. Defaulting safely to XML.");
        }
        return StorageType.XML;
    }
}
