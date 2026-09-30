package io.github.rbrisuda.jevface.examples.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rbrisuda.jevface.AgentDescriptor;
import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.spi.ChoiceSpec;
import io.github.rbrisuda.jevface.spi.ScoreSpec;
import org.junit.jupiter.api.Test;

/** Shows exactly what Jev will be asked, without calling it. */
class SupportTriageDefinitionTest {

    private final AgentDescriptor triage = Jevface.describe(SupportTriage.class);

    @Test
    void everyAbstractMethodIsOneQuestion() {
        assertThat(triage.name()).isEqualTo("support-triage");
        assertThat(triage.questionSpecs()).containsOnlyKeys(
                "is_urgent", "department", "frustration", "product", "product_stated", "refund_requested");
    }

    @Test
    void enumsBecomeOptionsAndLevels() {
        ChoiceSpec department = (ChoiceSpec) triage.questionSpecs().get("department");
        assertThat(department.options()).extracting(ChoiceSpec.Option::label)
                .containsExactly("billing", "technical", "sales", "shipping");

        ScoreSpec frustration = (ScoreSpec) triage.questionSpecs().get("frustration");
        assertThat(frustration.levels()).containsExactly(
                "Calm, neutral or friendly", "Annoyed or impatient", "Angry, threatening to leave or escalate");

        ChoiceSpec product = (ChoiceSpec) triage.questionSpecs().get("product");
        assertThat(product.options()).extracting(ChoiceSpec.Option::label).contains("mobile-app");
    }
}
