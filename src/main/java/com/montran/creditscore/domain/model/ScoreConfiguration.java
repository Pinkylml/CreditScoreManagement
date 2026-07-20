package com.montran.creditscore.domain.model;

/**
 * Immutable snapshot of all scoring parameters loaded from {@code credit-settings.properties}.
 * Passed to the formula engine on every evaluation so score weights and thresholds
 * can be adjusted at runtime without touching the code.
 */
public final class ScoreConfiguration {

    private final int utilizationWeight;
    private final int paymentHistoryWeight;
    private final int creditAgeWeight;
    private final int creditTypesWeight;
    private final int recentInquiriesWeight;
    private final double riskThresholdLow;
    private final double riskThresholdMedium;

    public ScoreConfiguration(int utilizationWeight, int paymentHistoryWeight,
            int creditAgeWeight, int creditTypesWeight,
            int recentInquiriesWeight,
            double riskThresholdLow, double riskThresholdMedium) {
        this.utilizationWeight = utilizationWeight;
        this.paymentHistoryWeight = paymentHistoryWeight;
        this.creditAgeWeight = creditAgeWeight;
        this.creditTypesWeight = creditTypesWeight;
        this.recentInquiriesWeight = recentInquiriesWeight;
        this.riskThresholdLow = riskThresholdLow;
        this.riskThresholdMedium = riskThresholdMedium;
    }

    public int getUtilizationWeight() {
        return utilizationWeight;
    }

    public int getPaymentHistoryWeight() {
        return paymentHistoryWeight;
    }

    public int getCreditAgeWeight() {
        return creditAgeWeight;
    }

    public int getCreditTypesWeight() {
        return creditTypesWeight;
    }

    public int getRecentInquiriesWeight() {
        return recentInquiriesWeight;
    }

    public double getRiskThresholdLow() {
        return riskThresholdLow;
    }

    public double getRiskThresholdMedium() {
        return riskThresholdMedium;
    }
}