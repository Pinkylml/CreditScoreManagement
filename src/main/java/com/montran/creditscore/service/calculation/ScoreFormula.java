package com.montran.creditscore.service.calculation;

import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.domain.model.User;

/**
 * Strategy interface for credit score calculation.
 * Swap implementations here to change the scoring model without touching the engine.
 */
public interface ScoreFormula {

    /**
     * Calculates a credit score for the given user.
     *
     * @param user   The user whose history will be evaluated.
     * @param config The scoring weights and thresholds to apply.
     * @return A score between 0.0 and 100.0.
     */
    double calculate(User user, ScoreConfiguration config);
}
