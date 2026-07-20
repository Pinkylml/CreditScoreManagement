package com.montran.creditscore.service.calculation;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.domain.model.User;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Calculates a credit score using five weighted factors:
 * utilization, payment history, credit age, credit type diversity, and recent inquiries.
 * The final score is clamped to [0.0, 100.0].
 */
public class WeightedScoreFormula implements ScoreFormula {

    @Override
    public double calculate(User user, ScoreConfiguration config) {
        if (user == null || config == null) {
            throw new IllegalArgumentException("User aggregate and ScoreConfiguration cannot be null.");
        }

        List<CreditHistoryRecord> history = user.getCreditHistory();

        if (history.isEmpty()) {
            return 0.0;
        }

        double utilizationPoints = computeUtilizationPoints(user, history, config.getUtilizationWeight());
        double paymentPoints = computePaymentHistoryPoints(history, config.getPaymentHistoryWeight());
        double agePoints = computeCreditAgePoints(history, config.getCreditAgeWeight());
        double typesPoints = computeCreditTypesPoints(history, config.getCreditTypesWeight());
        double inquiryPoints = computeInquiryPoints(history, config.getRecentInquiriesWeight());

        double totalScore = utilizationPoints + paymentPoints + agePoints + typesPoints + inquiryPoints;

        if (totalScore > 100.0)
            totalScore = 100.0;
        if (totalScore < 0.0)
            totalScore = 0.0;

        return Math.round(totalScore * 100.0) / 100.0;
    }

    /**
     * Computes utilization points: {@code (totalUsed / totalLimit) * maxWeight}, capped at maxWeight.
     * If the user has zero usage, returns 20% of the max as a baseline.
     */
    private double computeUtilizationPoints(User user, List<CreditHistoryRecord> history, int maxWeight) {
        double totalLimit = user.getTotalCreditLimit();
        double totalUsed = 0.0;

        for (CreditHistoryRecord record : history) {
            totalUsed += record.getAmount();
        }

        if (totalUsed == 0.0) {
            return maxWeight * 0.2; // baseline for zero utilization
        }

        double ratio = totalUsed / totalLimit;
        return Math.min(ratio * maxWeight, maxWeight);
    }

    /**
     * Computes payment history points: {@code (onTimePayments / totalPayments) * maxWeight}.
     * A payment is on time only if the settlement date is on or before the due date.
     * DEFAULTED records and records with no settlement date always count as missed payments.
     */
    private double computePaymentHistoryPoints(List<CreditHistoryRecord> history, int maxWeight) {
        if (history.isEmpty())
            return 0.0;

        int totalPayments = history.size();
        int onTimePayments = 0;

        for (CreditHistoryRecord record : history) {
            if (record.isDefaulted()) {
                continue;
            }

            if (record.getSettlementDate() != null && record.getDueDate() != null) {
                // Strictly on time: settled on or before the due date
                if (!record.getSettlementDate().after(record.getDueDate())) {
                    onTimePayments++;
                }
            }
        }

        return Math.min(((double) onTimePayments / totalPayments) * maxWeight, maxWeight);
    }

    /**
     * Computes credit age points: {@code (averageAgeInYears / 10) * maxWeight}, capped at maxWeight.
     * Age is measured from each record's due date to today.
     */
    private double computeCreditAgePoints(List<CreditHistoryRecord> history, int maxWeight) {
        if (history.isEmpty())
            return 0.0;

        long totalAgeInMillis = 0;
        Date current = new Date();

        for (CreditHistoryRecord record : history) {
            long diff = current.getTime() - record.getDueDate().getTime();
            if (diff > 0) {
                totalAgeInMillis += diff;
            }
        }

        double averageAgeInYears = (double) totalAgeInMillis / history.size() / (1000.0 * 60 * 60 * 24 * 365);
        return Math.min((averageAgeInYears / 10.0) * maxWeight, maxWeight);
    }

    /**
     * Computes credit diversity points: {@code uniqueTypes * 2.0}, capped at maxWeight.
     */
    private double computeCreditTypesPoints(List<CreditHistoryRecord> history, int maxWeight) {
        long uniqueTypesCount = history.stream()
                .map(CreditHistoryRecord::getTransactionType)
                .distinct()
                .count();

        double rawPoints = uniqueTypesCount * 2.0;
        return Math.min(rawPoints, maxWeight);
    }

    /**
     * Computes the recent-inquiry penalty: {@code recentCount * -2.0}, floored at {@code -maxPenalty}.
     * Only transactions with a due date in the last 24 months are counted.
     */
    private double computeInquiryPoints(List<CreditHistoryRecord> history, int maxPenalty) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -2);
        Date twoYearsAgo = cal.getTime();

        long recentInquiries = history.stream()
                .filter(record -> record.getDueDate().after(twoYearsAgo))
                .count();

        double penalty = recentInquiries * -2.0;
        return Math.max(penalty, -maxPenalty);
    }
}