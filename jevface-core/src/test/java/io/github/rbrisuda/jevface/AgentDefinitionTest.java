package io.github.rbrisuda.jevface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rbrisuda.jevface.TicketTriage.Department;
import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.Option;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import io.github.rbrisuda.jevface.spi.ChoiceSpec;
import io.github.rbrisuda.jevface.spi.NoulSpec;
import io.github.rbrisuda.jevface.spi.ScoreSpec;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentDefinitionTest {

    @Test
    void describesWhatIsSentToJev() {
        AgentDescriptor descriptor = Jevface.describe(TicketTriage.class);

        assertThat(descriptor.name()).isEqualTo("TicketTriage");
        assertThat(descriptor.model()).isEqualTo("jev-test");
        assertThat(descriptor.questionSpecs().get("is_urgent"))
                .isEqualTo(new NoulSpec("Does this convey urgency?", "Explicitly time-sensitive", null));
        assertThat(descriptor.questionSpecs().get("department")).isEqualTo(new ChoiceSpec("Which team should handle this?",
                List.of(new ChoiceSpec.Option("billing", "Payments, invoicing, refunds"),
                        new ChoiceSpec.Option("technical", "Bugs, outages, integrations"),
                        new ChoiceSpec.Option("sales-team", "Pricing, upgrades"))));
        assertThat(descriptor.questionSpecs().get("frustration"))
                .isEqualTo(new ScoreSpec("How frustrated is the customer?", List.of("Calm", "Frustrated", "Very angry")));
        assertThat(descriptor.questionSpecs().get("product_stated")).isEqualTo(NoulSpec.of("Does the customer name a product?"));
        assertThat(descriptor.question("department").labelOf(Department.SALES)).isEqualTo("sales-team");
        assertThat(descriptor.question("department").minConfidence()).isEqualTo(0.6);
    }

    @JevAgent
    interface Broken {
        @NoulQuestion("Needs input?")
        boolean withParameter(String input);

        boolean notAnnotated();

        @NoulQuestion("A noul")
        String wrongNoulType();

        @ChoiceQuestion("String choice without options")
        String noOptions();

        @ChoiceQuestion(value = "Gate without Optional", stated = "Is it mentioned?",
                options = {@Option(label = "a"), @Option(label = "b")})
        String statedWithoutOptional();

        @ScoreQuestion(value = "Too few levels", levels = {"only"})
        double oneLevel();

        @NoulQuestion(value = "Same id", key = "dup")
        boolean first();

        @NoulQuestion(value = "Same id", key = "dup")
        boolean second();

        @NoulQuestion("Optional noul")
        Optional<Boolean> optionalNoul();

        @NoulQuestion("Two annotations")
        @ScoreQuestion(value = "x", levels = {"a", "b"})
        double twoAnnotations();
    }

    @Test
    void reportsEveryProblemAtOnce() {
        assertThatThrownBy(() -> Jevface.describe(Broken.class))
                .isInstanceOfSatisfying(JevDefinitionException.class, e -> assertThat(e.problems()).containsExactlyInAnyOrder(
                        "withParameter(): question methods take no parameters; the prompt is the only input",
                        "notAnnotated(): is abstract but has no @NoulQuestion, @ChoiceQuestion or @ScoreQuestion; "
                                + "annotate it, or make it a default method",
                        "wrongNoulType(): @NoulQuestion cannot return java.lang.String; use boolean, double or NoulResult",
                        "noOptions(): a String choice needs options = {@Option(label = ...), ...}, or return an enum instead",
                        "statedWithoutOptional(): stated(...) means the answer can be absent, so the return type must be "
                                + "Optional<java.lang.String>",
                        "oneLevel(): a score needs between 2 and 10 levels, found 1",
                        "second(): question id 'dup' is already used by first()",
                        "optionalNoul(): @NoulQuestion cannot return Optional: a noul always has an answer",
                        "twoAnnotations(): has more than one question annotation"));
    }

    interface NotAnnotated {
        @NoulQuestion("?")
        boolean question();
    }

    @JevAgent
    interface Empty {
        default boolean nothing() {
            return false;
        }
    }

    @Test
    void rejectsNonAgents() {
        assertThatThrownBy(() -> Jevface.describe(NotAnnotated.class)).hasMessageContaining("missing the @JevAgent");
        assertThatThrownBy(() -> Jevface.describe(String.class)).hasMessageContaining("must be an interface");
        assertThatThrownBy(() -> Jevface.describe(Empty.class)).hasMessageContaining("declares no question methods");
    }

    enum Size { SMALL, LARGE }

    @JevAgent(minConfidence = 0.7)
    interface Inherits extends JevEvaluated {
        @ChoiceQuestion(value = "Which size?")
        Size size();

        @ChoiceQuestion(value = "Which size, loosely?", minConfidence = 0.0)
        Size looseSize();

        @ChoiceQuestion(value = "Enum with options", options = @Option(label = "x"))
        Size enumWithOptions();
    }

    @Test
    void enumChoicesMustNotDeclareOptions() {
        assertThatThrownBy(() -> Jevface.describe(Inherits.class))
                .hasMessageContaining("enumWithOptions(): options come from enum Size; remove options()");
    }

    @JevAgent(minConfidence = 0.7)
    interface Floors extends JevEvaluated {
        @ChoiceQuestion(value = "Which size?")
        Size size();

        @ChoiceQuestion(value = "Which size, loosely?", minConfidence = 0.0)
        Size looseSize();
    }

    @Test
    void agentMinConfidenceIsInheritedUnlessOverridden() {
        AgentDescriptor descriptor = Jevface.describe(Floors.class);

        assertThat(descriptor.question("size").minConfidence()).isEqualTo(0.7);
        assertThat(descriptor.question("loose_size").minConfidence()).isEqualTo(0.0);
        assertThat(descriptor.question("size").options()).containsOnlyKeys("small", "large");
    }
}
