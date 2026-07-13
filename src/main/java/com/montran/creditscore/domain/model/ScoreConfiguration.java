package com.montran.creditscore.domain.model;

/**
 * @docs Domain Value Object encapsulating the mathematical weights applied during credit score evaluation.
 */
public final class ScoreConfiguration {

    private final int utilizationWeight;
    private final int paymentHistoryWeight;
    private final int creditAgeWeight;
    private final int creditTypesWeight;
    private final int recentInquiriesWeight;

    public ScoreConfiguration(int utilizationWeight, int paymentHistoryWeight,
                              int creditAgeWeight, int creditTypesWeight,
                              int recentInquiriesWeight) {
        this.utilizationWeight = utilizationWeight;
        this.paymentHistoryWeight = paymentHistoryWeight;
        this.creditAgeWeight = creditAgeWeight;
        this.creditTypesWeight = creditTypesWeight;
        this.recentInquiriesWeight = recentInquiriesWeight;
    }

    /**
     * @docs Gets the maximum point allocation for the credit utilization ratio.
     * @return Integer weight value.
     */
    public int getUtilizationWeight() {
        return utilizationWeight;
    }

    /**
     * @docs Gets the maximum point allocation for on-time payment history.
     * @return Integer weight value.
     */
    public int getPaymentHistoryWeight() {
        return paymentHistoryWeight;
    }

    /**
     * @docs Gets the maximum point allocation for the average age of credit accounts.
     * @return Integer weight value.
     */
    public int getCreditAgeWeight() {
        return creditAgeWeight;
    }

    /**
     * @docs Gets the maximum point allocation for diverse credit type ownership.
     * @return Integer weight value.
     */
    public int getCreditTypesWeight() {
        return creditTypesWeight;
    }

    /**
     * @docs Gets the maximum penalty allocation for recent hard inquiries.
     * @return Integer weight value.
     */
    public int getRecentInquiriesWeight() {
        return recentInquiriesWeight;
    }
}