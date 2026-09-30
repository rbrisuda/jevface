package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.ChoiceResult;
import io.github.rbrisuda.jevface.ScoreResult;
import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import java.util.Optional;

/**
 * The whole "AI" of this application. Nobody implements this interface: each abstract method is one
 * question Jev answers about the ticket (all in a single call), the return type decides the kind of
 * question and how the answer is mapped. Default methods are plain Java business rules on top.
 */
@JevAgent(name = "support-triage", description = "Triage of an incoming customer support ticket")
public interface SupportTriage {

    @NoulQuestion(value = "Does the customer need help urgently?",
            whenTrue = "Time-sensitive: outage, blocked work, money at risk or an explicit deadline",
            whenFalse = "No urgency expressed")
    boolean isUrgent();

    @ChoiceQuestion("Which team should handle this ticket?")
    ChoiceResult<Department> department();

    @ScoreQuestion("How frustrated is the customer?")
    ScoreResult<Frustration> frustration();

    /** Empty unless the customer actually names a product and Jev is reasonably sure which. */
    @ChoiceQuestion(value = "Which product is the ticket about?",
            stated = "Does the customer mention a specific product?", minConfidence = 0.5)
    Optional<Product> product();

    /** Probability (0..1) instead of a boolean, to show the raw noul value. */
    @NoulQuestion("Does the customer ask for money back?")
    double refundRequested();

    default Priority priority() {
        boolean angry = frustration().level() == Frustration.ANGRY;
        if (isUrgent()) {
            return angry ? Priority.P1 : Priority.P2;
        }
        return angry ? Priority.P2 : Priority.P3;
    }

    /** Low confidence in the routing decision means a human should take a look. */
    default boolean needsHumanReview() {
        return department().confidence() < 0.6;
    }
}
