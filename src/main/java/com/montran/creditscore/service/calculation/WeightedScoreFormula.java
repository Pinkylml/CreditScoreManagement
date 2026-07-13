package com.montran.creditscore.service.calculation;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.domain.model.User;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @docs Concrete formula strategy executing the standardized scoring
 *       algorithms.
 *       <p>
 *       <b>Design Justification:</b> Encapsulates the specific mathematical
 *       logic required to evaluate a user's credit profile based on
 *       utilization, payment history, credit age, diversity, and recent
 *       inquiries.
 *       </p>
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
        double paymentPoints = computePaymentHistoryPoints(history, config.getPaymentHistoryWeight(),
                config.getLatePaymentGraceDays());
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
     * @docs Calculates the utilization ratio points by comparing the total used
     *       credit against the user's specific total credit limit.
     * @param user      The aggregate root containing the total personal credit
     *                  limit.
     * @param history   The immutable list of user transaction records.
     * @param maxWeight The configurable maximum points allowed for this category.
     * @return Calculated double representing points awarded for credit utilization.
     */
    private double computeUtilizationPoints(User user, List<CreditHistoryRecord> history, int maxWeight) {
        double totalLimit = user.getTotalCreditLimit();
        double totalUsed = 0.0;

        for (CreditHistoryRecord record : history) {
            if (record.isDefaulted()) {
                totalUsed += record.getAmount();
            }
        }

        if (totalUsed == 0.0) {
            return maxWeight * 0.2; // Baseline assignment for zero utilization
        }

        double ratio = totalUsed / totalLimit;
        return ratio * maxWeight;
    }

    /**
     * @docs Evaluates payment reliability by comparing settlement dates against
     *       contractual due dates, factoring in the allowable system grace period.
     * @param history   The immutable list of user transaction records.
     * @param maxWeight The configurable maximum points allowed for this category.
     * @param graceDays The operational grace period in days before a payment is
     *                  officially penalized as late.
     * @return Calculated double representing points awarded for payment history.
     */
    private double computePaymentHistoryPoints(List<CreditHistoryRecord> history, int maxWeight, int graceDays) {
        if (history.isEmpty())
            return 0.0;

        int totalPayments = history.size();
        int onTimePayments = 0;

        for (CreditHistoryRecord record : history) {
            if (record.isDefaulted()) {
                continue; // Defaulted status is automatically penalized as a missed payment
            }

            if (record.getSettlementDate() != null && record.getDueDate() != null) {
                long diffInMillis = record.getSettlementDate().getTime() - record.getDueDate().getTime();
                long daysLate = diffInMillis / (1000 * 60 * 60 * 24);

                if (daysLate <= graceDays) {
                    onTimePayments++;
                }
            }
        }

        return ((double) onTimePayments / totalPayments) * maxWeight;
    }

    /**
     * @docs Calculates the average lifespan of all recorded credit instances using
     *       their original due dates as the baseline for the timeline calculation.
     * @param history   The immutable list of user transaction records.
     * @param maxWeight The configurable maximum points allowed for this category.
     * @return Calculated double representing points awarded for the length of
     *         credit history.
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
        return (averageAgeInYears / 10.0) * maxWeight;
    }

    /**
     * @docs Assesses portfolio diversification by counting the number of unique
     *       financial transaction types present in the user's history.
     * @param history   The immutable list of user transaction records.
     * @param maxWeight The configurable maximum points allowed for this category.
     * @return Calculated double representing points awarded for credit variance.
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
     * @docs Calculates the negative penalty metric for recent credit inquiries
     *       spanning the last 24 months, utilizing the contractual due date.
     * @param history    The immutable list of user transaction records.
     * @param maxPenalty The configurable maximum penalty constraint (absolute
     *                   value) allowed.
     * @return Calculated double representing negative points to be subtracted from
     *         the total score.
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