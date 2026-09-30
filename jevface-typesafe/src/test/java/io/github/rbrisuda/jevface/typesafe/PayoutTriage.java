package io.github.rbrisuda.jevface.typesafe;

import io.github.rbrisuda.jevface.ScoreResult;
import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.Option;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import java.util.Optional;

/** The ticket from the Spring AI TypeSafe blog post, as a typed agent. */
@JevAgent
public interface PayoutTriage {

    enum Department {
        @Option(description = "Payments, invoicing, refunds") BILLING,
        @Option(description = "Bugs, outages, integrations") TECHNICAL,
        @Option(description = "Pricing, upgrades, new accounts") SALES
    }

    @NoulQuestion(value = "Does this convey urgency?", whenTrue = "Explicitly time-sensitive", whenFalse = "No urgency expressed")
    boolean isUrgent();

    @ChoiceQuestion("Which team should handle this?")
    Department department();

    @ScoreQuestion(value = "How frustrated is the customer?", levels = {"Calm", "Frustrated", "Very angry"})
    ScoreResult<String> frustration();

    @ChoiceQuestion(value = "Which product is affected?", stated = "Does the customer name the affected product?",
            options = {@Option(label = "payouts"), @Option(label = "cards", description = "Debit and credit cards")})
    Optional<String> product();
}
