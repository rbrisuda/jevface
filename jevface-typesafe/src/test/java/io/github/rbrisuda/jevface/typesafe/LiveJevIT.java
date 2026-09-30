package io.github.rbrisuda.jevface.typesafe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rbrisuda.jevface.Jevface;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Calls the real Jev API. Gated twice so an exported key never turns an ordinary build into a
 * billed one: run with {@code TYPESAFE_API_KEY=... ./gradlew liveTest}.
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "TYPESAFE_API_KEY", matches = ".+")
class LiveJevIT {

    @Test
    void triagesTheBlogTicket() {
        Jevface jev = Jevface.create(TypeSafeJudgmentEngine.fromEnvironment());

        PayoutTriage triage = jev.evaluate(PayoutTriage.class, "Help! My payouts have been failing for 3 days.");

        System.out.println(triage + " " + Jevface.evaluationOf(triage).latency());
        assertThat(triage.isUrgent()).isTrue();
        assertThat(triage.department()).isEqualTo(PayoutTriage.Department.BILLING);
    }
}
