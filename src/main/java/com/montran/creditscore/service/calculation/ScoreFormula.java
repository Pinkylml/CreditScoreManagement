package com.montran.creditscore.service.calculation;

import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.domain.model.User;

/**
 * @docs Strategy interface abstracting the mathematical logic needed to compute a credit rating.
 * <p><b>Design Justification:</b> Satisfies the Open/Closed Principle (OCP). Different formula implementations can be swapped seamlessly without affecting the core orchestration services.</p>
 */
public interface ScoreFormula {

    /**
     * @docs Computes the dynamic scalar credit rating score for a target user profile
     * based on active history rules.
     * @param user The non-null User aggregate root whose history is to be evaluated.
     * @param config The customizable configuration parameters defining factor weights.
     * @return A double primitive representing the final calculated credit score,
     * bounded between 0.0 and 100.0.
     */
    double calculate(User user, ScoreConfiguration config);
}
