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
     * @docs Classifies the calculated credit score into standard domain risk profiles using dynamic thresholds.
     */
    public static RiskLevel classify(double score, double lowThreshold, double mediumThreshold) {
        if (score >= lowThreshold) {
            return RiskLevel.LOW;
        } else if (score >= mediumThreshold) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }
}