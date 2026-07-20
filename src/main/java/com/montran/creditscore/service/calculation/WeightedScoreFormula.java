package com.montran.creditscore.service.calculation;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.domain.model.TransactionType;
import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.exception.CreditCalculationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calculates a credit score using five weighted factors:
 * utilization, payment history, credit age, credit type diversity, and recent inquiries.
 * The final score is clamped to [0.0, 100.0].
 *
 * <p>{@link TransactionType#INQUIRY} records participate <em>only</em> in the inquiry-penalty
 * calculation. They are excluded from utilization, payment history, credit age, and diversity
 * scoring because an inquiry is not a credit instrument and carries no monetary obligation.</p>
 */
public class WeightedScoreFormula implements ScoreFormula {

    @Override
    public double calculate(User user, ScoreConfiguration config) {
        if (user == null || config == null) {
            throw new CreditCalculationException(
                    "User aggregate and ScoreConfiguration must not be null for score calculation.");
        }

        List<CreditHistoryRecord> history = user.getCreditHistory();

        if (history.isEmpty()) {
            return 0.0;
        }

        // Credit instruments only – INQUIRY records are handled exclusively by computeInquiryPoints.
        List<CreditHistoryRecord> creditHistory = history.stream()
                .filter(r -> r.getTransactionType() != TransactionType.INQUIRY)
                .collect(Collectors.toList());

        double utilizationPoints = computeUtilizationPoints(user, creditHistory, config.getUtilizationWeight());
        double paymentPoints = computePaymentHistoryPoints(creditHistory, config.getPaymentHistoryWeight());
        double agePoints = computeCreditAgePoints(creditHistory, config.getCreditAgeWeight());
        double typesPoints = computeCreditTypesPoints(creditHistory, config.getCreditTypesWeight());
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
     * Monetary arithmetic uses {@link BigDecimal} with {@link RoundingMode#HALF_UP} (scale 2)
     * to avoid binary floating-point drift when summing amounts.
     * If the user has zero usage, returns 20% of the max as a baseline.
     * INQUIRY records are already excluded from the supplied list.
     */
    private double computeUtilizationPoints(User user, List<CreditHistoryRecord> creditHistory, int maxWeight) {
        BigDecimal totalLimit = user.getTotalCreditLimit();
        BigDecimal totalUsed = BigDecimal.ZERO;

        for (CreditHistoryRecord record : creditHistory) {
            totalUsed = totalUsed.add(record.getAmount());
        }

        if (totalUsed.compareTo(BigDecimal.ZERO) == 0) {
            return maxWeight * 0.2; // baseline for zero utilization
        }

        // Divide with scale=2 and HALF_UP to get a precise ratio, then convert to double for score arithmetic.
        BigDecimal ratio = totalUsed.divide(totalLimit, 2, RoundingMode.HALF_UP);
        double ratioDouble = ratio.doubleValue();
        return Math.min(ratioDouble * maxWeight, maxWeight);
    }

    /**
     * Computes payment history points: {@code (onTimePayments / totalPayments) * maxWeight}.
     * A payment is on time only if the settlement date is on or before the due date.
     * DEFAULTED records and records with no settlement date always count as missed payments.
     * INQUIRY records are already excluded from the supplied list.
     */
    private double computePaymentHistoryPoints(List<CreditHistoryRecord> creditHistory, int maxWeight) {
        if (creditHistory.isEmpty())
            return 0.0;

        int totalPayments = creditHistory.size();
        int onTimePayments = 0;

        for (CreditHistoryRecord record : creditHistory) {
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
     * INQUIRY records are already excluded from the supplied list.
     */
    private double computeCreditAgePoints(List<CreditHistoryRecord> creditHistory, int maxWeight) {
        if (creditHistory.isEmpty())
            return 0.0;

        long totalAgeInMillis = 0;
        Date current = new Date();

        for (CreditHistoryRecord record : creditHistory) {
            long diff = current.getTime() - record.getDueDate().getTime();
            if (diff > 0) {
                totalAgeInMillis += diff;
            }
        }

        double averageAgeInYears = (double) totalAgeInMillis / creditHistory.size() / (1000.0 * 60 * 60 * 24 * 365);
        return Math.min((averageAgeInYears / 10.0) * maxWeight, maxWeight);
    }

    /**
     * Computes credit diversity points: {@code uniqueTypes * 2.0}, capped at maxWeight.
     * INQUIRY records are already excluded from the supplied list.
     */
    private double computeCreditTypesPoints(List<CreditHistoryRecord> creditHistory, int maxWeight) {
        long uniqueTypesCount = creditHistory.stream()
                .map(CreditHistoryRecord::getTransactionType)
                .distinct()
                .count();

        double rawPoints = uniqueTypesCount * 2.0;
        return Math.min(rawPoints, maxWeight);
    }

    /**
     * Computes the recent-inquiry penalty: {@code recentCount * -2.0}, floored at {@code -maxPenalty}.
     * <p>Only transactions whose type is strictly {@link TransactionType#INQUIRY} <em>and</em>
     * whose due date falls within the last 24 months are counted.
     * Standard credit instruments (loans, credit cards, etc.) are never penalised here.</p>
     */
    private double computeInquiryPoints(List<CreditHistoryRecord> history, int maxPenalty) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -2);
        Date twoYearsAgo = cal.getTime();

        long recentInquiries = history.stream()
                .filter(record -> record.getTransactionType() == TransactionType.INQUIRY)
                .filter(record -> record.getDueDate().after(twoYearsAgo))
                .count();

        double penalty = recentInquiries * -2.0;
        return Math.max(penalty, -maxPenalty);
    }
}