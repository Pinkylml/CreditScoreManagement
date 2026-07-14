package com.montran.creditscore.service.risk;

import com.montran.creditscore.domain.model.RiskLevel;

/**
 * Maps a numeric credit score to a {@link RiskLevel} using configurable thresholds.
 * Thresholds come from {@link com.montran.creditscore.domain.model.ScoreConfiguration}.
 */
public final class RiskClassifier {

    private RiskClassifier() {
        throw new UnsupportedOperationException("Utility configuration class cannot be instantiated directly.");
    }

    /**
     * Classifies a score into LOW, MEDIUM, or HIGH risk.
     *
     * @param score           The computed credit score (0–100).
     * @param lowThreshold    Scores at or above this are LOW risk.
     * @param mediumThreshold Scores at or above this (but below lowThreshold) are MEDIUM risk.
     *                        Scores below this are HIGH risk.
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