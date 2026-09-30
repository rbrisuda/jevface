package io.github.rbrisuda.jevface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.github.rbrisuda.jevface.TicketTriage.Department;
import io.github.rbrisuda.jevface.TicketTriage.Frustration;
import io.github.rbrisuda.jevface.spi.JudgmentRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JevfaceTest {

    @Test
    void answersEveryQuestionInOneCallAndMapsThemToTypedValues() {
        Answers engine = Answers.typicalTicket();

        TicketTriage triage = Jevface.create(engine).evaluate(TicketTriage.class, "Payouts failing for 3 days!");

        assertThat(engine.requests).hasSize(1);
        assertThat(triage.isUrgent()).isTrue();
        assertThat(triage.refundRequested()).isEqualTo(0.1);
        assertThat(triage.churnRisk()).isEqualTo(new NoulResult(0.4, 0.5));
        assertThat(triage.churnRisk().isTrue()).isFalse();
        assertThat(triage.department()).isEqualTo(Department.BILLING);
        assertThat(triage.channel()).isEqualTo("email");
        assertThat(triage.product()).contains("payouts");
        assertThat(triage.frustrationLevel()).isEqualTo(Frustration.FRUSTRATED);
        assertThat(triage.politeness()).isEqualTo(0.3);
        assertThat(triage.politenessIndex()).isEqualTo(2);
    }

    @Test
    void resultTypesExposeProbabilitiesAndConfidence() {
        TicketTriage triage = Jevface.create(Answers.typicalTicket()).evaluate(TicketTriage.class, "prompt");

        ChoiceResult<Department> department = triage.departmentDetail();
        assertThat(department.value()).isEqualTo(Department.BILLING);
        assertThat(department.confidence()).isEqualTo(0.82);
        assertThat(department.probabilities()).containsExactly(
                Map.entry(Department.BILLING, 0.88), Map.entry(Department.TECHNICAL, 0.12), Map.entry(Department.SALES, 0.0));

        ScoreResult<Frustration> frustration = triage.frustration();
        assertThat(frustration.value()).isEqualTo(1.6);
        assertThat(frustration.level()).isEqualTo(Frustration.VERY_ANGRY);
        assertThat(frustration.levelIndex()).isEqualTo(2);
        assertThat(frustration.probabilityOf(Frustration.FRUSTRATED)).isEqualTo(0.4);
    }

    @Test
    void defaultMethodsComposeAnswersInCode() {
        TicketTriage triage = Jevface.create(Answers.typicalTicket()).evaluate(TicketTriage.class, "prompt");

        assertThat(triage.needsEscalation()).isTrue();
    }

    @Test
    void booleanNoulUsesTheQuestionThreshold() {
        TicketTriage triage = Jevface.create(Answers.typicalTicket().noul("is_urgent", 0.69))
                .evaluate(TicketTriage.class, "prompt");

        assertThat(triage.isUrgent()).isFalse();
    }

    @Test
    void statedGateBelowThresholdMakesTheAnswerAbsent() {
        TicketTriage triage = Jevface.create(Answers.typicalTicket().noul("product_stated", 0.2))
                .evaluate(TicketTriage.class, "prompt");

        assertThat(triage.product()).isEmpty();
    }

    @Test
    void lowConfidenceEmptiesOptionalsAndFailsPlainValuesOnlyWhenRead() {
        Answers engine = Answers.typicalTicket()
                .choice("product", "cards", 0.3, Map.of("cards", 0.6, "payouts", 0.4))
                .choice("department", "technical", 0.4, Map.of("technical", 0.6, "billing", 0.4));

        TicketTriage triage = Jevface.create(engine).evaluate(TicketTriage.class, "prompt");

        assertThat(triage.product()).isEmpty();
        assertThat(triage.isUrgent()).isTrue();
        assertThatThrownBy(triage::department)
                .isInstanceOf(LowConfidenceException.class)
                .hasMessageContaining("'department'")
                .hasMessageContaining("0.40")
                .hasMessageContaining("0.60");
    }

    @Test
    void sendsAgentNameModelStateAndSnakeCaseQuestionIds() {
        Answers engine = Answers.typicalTicket();

        Jevface.create(engine).evaluate(TicketTriage.class, "Payouts failing!");

        JudgmentRequest request = engine.requests.getFirst();
        assertThat(request.agentName()).isEqualTo("TicketTriage");
        assertThat(request.model()).isEqualTo("jev-test");
        assertThat(request.state()).isEqualTo("Payouts failing!");
        assertThat(request.questions()).containsOnlyKeys("channel", "churn_risk", "department", "department_detail",
                "frustration", "frustration_level", "is_urgent", "politeness", "politeness_index", "product",
                "product_stated", "refund_requested");
    }

    @Test
    void exposesEvaluationMetadataAndReadableToString() {
        TicketTriage triage = Jevface.create(Answers.typicalTicket()).evaluate(TicketTriage.class, "prompt");

        EvaluationMetadata metadata = Jevface.evaluationOf(triage);
        assertThat(metadata.model()).isEqualTo("jev-1.13.0");
        assertThat(metadata.requestId()).isEqualTo("req-1");
        assertThat(metadata.usage().totalTokens()).isEqualTo(340);
        assertThat(metadata.state()).isEqualTo("prompt");
        assertThat(triage.toString()).startsWith("TicketTriage{").contains("is_urgent=0.95", "department=billing (confidence 0.82)");
        assertThat(triage.equals(triage)).isTrue();
        assertThat(triage).isNotEqualTo(Jevface.create(Answers.typicalTicket()).evaluate(TicketTriage.class, "prompt"));
    }

    @Test
    void evaluateAllKeepsOrder() {
        Answers engine = Answers.typicalTicket();

        List<TicketTriage> results = Jevface.create(engine).client(TicketTriage.class).evaluateAll(List.of("a", "b"));

        assertThat(results).hasSize(2);
        assertThat(Jevface.evaluationOf(results.get(1)).state()).isEqualTo("b");
    }

    @Test
    void clientsAreCachedPerAgentType() {
        Jevface jev = Jevface.create(Answers.typicalTicket());

        assertThat(jev.client(TicketTriage.class)).isSameAs(jev.client(TicketTriage.class));
    }

    @Test
    void rejectsBlankPrompts() {
        assertThatThrownBy(() -> Jevface.create(Answers.typicalTicket()).evaluate(TicketTriage.class, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsWhenEngineOmitsAnAnswer() {
        Answers engine = Answers.typicalTicket();
        engine.answers.remove("channel");

        assertThatThrownBy(() -> Jevface.create(engine).evaluate(TicketTriage.class, "prompt"))
                .isInstanceOf(JevEvaluationException.class)
                .hasMessageContaining("no answer for question 'channel'")
                .hasMessageContaining("TicketTriage.channel()");
    }

    @Test
    void failsWhenEngineAnswersAnUnknownOption() {
        Answers engine = Answers.typicalTicket().choice("channel", "fax", 0.9, Map.of());

        assertThatThrownBy(() -> Jevface.create(engine).evaluate(TicketTriage.class, "prompt"))
                .isInstanceOf(JevEvaluationException.class)
                .hasMessageContaining("'fax'");
    }

    @Test
    void failsWhenEngineAnswersWithTheWrongPrimitive() {
        Answers engine = Answers.typicalTicket().noul("department", 0.5);

        assertThatThrownBy(() -> Jevface.create(engine).evaluate(TicketTriage.class, "prompt"))
                .isInstanceOf(JevEvaluationException.class)
                .hasMessageContaining("Expected a ChoiceAnswer for question 'department'");
    }

    @Test
    void wrapsEngineFailures() {
        Jevface jev = Jevface.create(request -> {
            throw new IllegalStateException("HTTP 529 overloaded");
        });

        assertThatThrownBy(() -> jev.evaluate(TicketTriage.class, "prompt"))
                .isInstanceOf(JevEvaluationException.class)
                .hasMessageContaining("TicketTriage")
                .hasMessageContaining("HTTP 529")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void scoreValuesAreClampedToTheRubric() {
        TicketTriage triage = Jevface.create(Answers.typicalTicket().score("politeness_index", 7.0, 1.0, Map.of()))
                .evaluate(TicketTriage.class, "prompt");

        assertThat(triage.politenessIndex()).isEqualTo(2);
        assertThat(triage.frustration().confidence()).isCloseTo(0.7, within(1e-9));
    }
}
