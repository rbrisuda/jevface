package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.JevEvaluationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** {@code ./gradlew :examples:customer-support:bootRun --args="'My payouts failed for 3 days!'"} */
@Component
class TriageCommandLine implements ApplicationRunner {

    private final TicketRouter router;

    TriageCommandLine(TicketRouter router) {
        this.router = router;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            for (String ticket : args.getNonOptionArgs()) {
                System.out.println(ticket + System.lineSeparator() + "  -> " + router.route(ticket));
            }
        } catch (JevEvaluationException exception) {
            System.err.println(exception.getMessage());
        }
    }
}
