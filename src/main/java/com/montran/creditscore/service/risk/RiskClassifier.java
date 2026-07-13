package com.montran.creditscore.service.risk;

import com.montran.creditscore.domain.model.RiskLevel;

/**
 * @docs Utility class evaluating a numeric credit rating against operational business boundaries to resolve risk levels.
 * <p><b>Design Justification:</b> Encapsulates standard threshold classification patterns completely separate from data serialization or formula processing structures. This prevents branching logic pollution inside the calculation services.</p>
 */
public final class RiskClassifier {

    /**
     * @docs Private constructor to enforce static utility usage.
     */
    private RiskClassifier() {
        throw new UnsupportedOperationException("Utility configuration class cannot be instantiated directly.");
    }

    /**
     * @docs Resolves the definitive RiskLevel matching a provided scalar credit score.
     * @param score The double rating primitive value bounded between 0.0 and 100.0.
     * @return The resolved RiskLevel enum matching the predefined operational classification ranges.
     */
    public static RiskLevel classify(double score) {
        if (score >= 75.0) {
            return RiskLevel.LOW;
        } else if (score >= 50.0) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }
}