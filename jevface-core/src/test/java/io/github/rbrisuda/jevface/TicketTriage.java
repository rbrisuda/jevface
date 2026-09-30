package io.github.rbrisuda.jevface;

import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.Option;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import java.util.Optional;

@JevAgent(description = "Ticket triage used in tests", model = "jev-test")
public interface TicketTriage {

    enum Department {
        @Option(description = "Payments, invoicing, refunds") BILLING,
        @Option(description = "Bugs, outages, integrations") TECHNICAL,
        @Option(label = "sales-team", description = "Pricing, upgrades") SALES
    }

    enum Frustration {
        @Option(description = "Calm") CALM,
        @Option(description = "Frustrated") FRUSTRATED,
        @Option(description = "Very angry") VERY_ANGRY
    }

    @NoulQuestion(value = "Does this convey urgency?", whenTrue = "Explicitly time-sensitive", threshold = 0.7)
    boolean isUrgent();

    @NoulQuestion("Is the customer asking for money back?")
    double refundRequested();

    @NoulQuestion("Is the customer threatening to leave?")
    NoulResult churnRisk();

    @ChoiceQuestion(value = "Which team should handle this?", minConfidence = 0.6)
    Department department();

    @ChoiceQuestion(value = "Which team should handle this?", key = "department_detail")
    ChoiceResult<Department> departmentDetail();

    @ChoiceQuestion(value = "Which channel did the customer use?",
            options = {@Option(label = "email"), @Option(label = "phone", description = "A phone call")})
    String channel();

    @ChoiceQuestion(value = "Which product is this about?", stated = "Does the customer name a product?",
            minConfidence = 0.5, options = {@Option(label = "cards"), @Option(label = "payouts")})
    Optional<String> product();

    @ScoreQuestion("How frustrated is the customer?")
    ScoreResult<Frustration> frustration();

    @ScoreQuestion(value = "How frustrated is the customer?", key = "frustration_level")
    Frustration frustrationLevel();

    @ScoreQuestion(value = "How polite is the message?", levels = {"Rude", "Neutral", "Polite"})
    double politeness();

    @ScoreQuestion(value = "How polite is the message?", levels = {"Rude", "Neutral", "Polite"}, key = "politeness_index")
    int politenessIndex();

    default boolean needsEscalation() {
        return isUrgent() && frustration().value() >= 1.5;
    }
}
