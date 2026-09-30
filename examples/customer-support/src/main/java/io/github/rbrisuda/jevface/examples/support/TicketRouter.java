package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.EvaluationMetadata;
import io.github.rbrisuda.jevface.JevClient;
import io.github.rbrisuda.jevface.Jevface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

/**
 * Plain business logic: no prompts, no JSON, no if/else on the ticket text.
 */
@Service
public class TicketRouter {

    static final String HUMAN_REVIEW_QUEUE = "human-review";

    private final JevClient<SupportTriage> triage;

    public TicketRouter(JevClient<SupportTriage> triage) {
        this.triage = triage;
    }

    public RoutingDecision route(String ticket) {
        SupportTriage answers = triage.evaluate(ticket);

        Department department = answers.department().value();
        List<String> notes = new ArrayList<>();
        String queue = department.name().toLowerCase(Locale.ROOT);
        if (answers.needsHumanReview()) {
            notes.add(String.format(Locale.ROOT, "Routing is uncertain (%.0f%% %s), sent to human review",
                    answers.department().confidence() * 100, queue));
            queue = HUMAN_REVIEW_QUEUE;
        }
        boolean refund = answers.refundRequested() >= 0.5;
        if (refund && department != Department.BILLING) {
            notes.add("Refund requested: loop in billing");
        }
        if (answers.product().isEmpty()) {
            notes.add("No product identified: ask the customer");
        }

        EvaluationMetadata evaluation = Jevface.evaluationOf(answers);
        return new RoutingDecision(queue, answers.priority(), department, answers.department().confidence(),
                answers.frustration().level(), answers.product().orElse(null), refund, List.copyOf(notes),
                evaluation.model(), evaluation.latency().toMillis());
    }
}
