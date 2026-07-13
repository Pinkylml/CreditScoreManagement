package com.montran.creditscore.domain.infrastructure.config;

import com.montran.creditscore.domain.model.ScoreConfiguration;

import java.io.InputStream;
import java.util.Properties;

/**
 * @docs Infrastructure component dedicated to parsing external configuration files for dynamic calculation adjustments.
 * <p><b>Design Justification:</b> Adheres to the Single Responsibility Principle (SRP). It exclusively manages file I/O and properties extraction, isolating these concerns from the core calculation strategies.</p>
 */
public class PropertyWeightLoader {

    private static final String PROPERTIES_FILE = "credit-settings.properties";

    /**
     * @docs Private constructor to enforce static utility usage.
     */
    private PropertyWeightLoader(){
    }

    /**
     * @docs Loads the configuration weights from the classpath.
     * Applies safe defaults if the file is missing or properties are malformed.
     * @return A fully populated ScoreConfiguration domain object.
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

        // Parse with strict fallbacks to the standard specification rules
        int utilization = parseProperty(props, "weight.credit.utilization", 30);
        int paymentHistory = parseProperty(props, "weight.payment.history", 35);
        int creditAge = parseProperty(props, "weight.credit.age", 15);
        int creditTypes = parseProperty(props, "weight.credit.types", 10);
        int recentInquiries = parseProperty(props, "weight.recent.inquiries", 10);
        int graceDays = parseProperty(props, "late.payment.grace.days", 30);

        return new ScoreConfiguration(utilization, paymentHistory, creditAge, creditTypes, recentInquiries, graceDays);
    }

    /**
     * @docs Safely extracts and parses an integer property, returning a fallback default if parsing fails.
     */
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
}
