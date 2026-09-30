package io.github.rbrisuda.jevface.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rbrisuda.jevface.ChoiceResult;
import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StubJudgmentEngineTest {

    @JevAgent
    interface Review {
        enum Sentiment { NEGATIVE, NEUTRAL, POSITIVE }

        @NoulQuestion("Does the review mention shipping?")
        boolean mentionsShipping();

        @ChoiceQuestion("Overall sentiment?")
        ChoiceResult<Sentiment> sentiment();

        @ScoreQuestion(value = "How many stars would the reviewer give?", levels = {"1", "2", "3", "4", "5"})
        int stars();

        @ChoiceQuestion(value = "Which size does the reviewer mention?", stated = "Is a size mentioned?",
                options = {@io.github.rbrisuda.jevface.annotation.Option(label = "small"), @io.github.rbrisuda.jevface.annotation.Option(label = "large")})
        Optional<String> size();
    }

    private final StubJudgmentEngine engine = new StubJudgmentEngine();
    private final Jevface jev = Jevface.create(engine);

    @Test
    void answersFromStubsAndRecordsRequests() {
        engine.forAgent(Review.class)
                .noul(Review::mentionsShipping, 0.9)
                .choice(Review::sentiment, Review.Sentiment.POSITIVE, 0.8)
                .scoreLevel(Review::stars, "4", 0.95)
                .choice(Review::size, "large");

        Review review = jev.evaluate(Review.class, "Arrived fast, fits great in L.");

        assertThat(review.mentionsShipping()).isTrue();
        assertThat(review.sentiment().value()).isEqualTo(Review.Sentiment.POSITIVE);
        assertThat(review.sentiment().confidence()).isEqualTo(0.8);
        assertThat(review.sentiment().probabilityOf(Review.Sentiment.NEGATIVE)).isEqualTo(0.1, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(review.stars()).isEqualTo(3);
        assertThat(review.size()).as("stated defaults to 1.0").contains("large");
        assertThat(engine.requests()).singleElement()
                .satisfies(request -> assertThat(request.state()).isEqualTo("Arrived fast, fits great in L."));
    }

    @Test
    void statedGateCanHideAnAnswer() {
        engine.forAgent(Review.class)
                .noul(Review::mentionsShipping, 0.1)
                .choice(Review::sentiment, Review.Sentiment.NEUTRAL)
                .score(Review::stars, 2.0)
                .choice(Review::size, "small")
                .stated(Review::size, 0.2);

        assertThat(jev.evaluate(Review.class, "ok").size()).isEmpty();
    }

    @Test
    void rejectsStubsOfTheWrongQuestionType() {
        assertThatThrownBy(() -> engine.forAgent(Review.class).noul(Review::sentiment, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sentiment() is a Choice question, not a Noul");
        assertThatThrownBy(() -> engine.forAgent(Review.class).stated(Review::stars, 0.5))
                .hasMessage("stars() has no stated(...) gate");
        assertThatThrownBy(() -> engine.forAgent(Review.class).score(Review::stars, 7))
                .hasMessage("score must be between 0 and 4 for 'stars'");
    }

    @Test
    void failsClearlyOnMissingStubs() {
        engine.forAgent(Review.class).noul(Review::mentionsShipping, 0.5);

        assertThatThrownBy(() -> jev.evaluate(Review.class, "ok"))
                .isInstanceOf(JevEvaluationException.class)
                .rootCause()
                .hasMessageContaining("No stubbed answer for question 'sentiment'");
    }

    @Test
    void resetForgetsEverything() {
        engine.forAgent(Review.class).noul(Review::mentionsShipping, 0.5);
        engine.reset();

        assertThat(engine.requests()).isEmpty();
        assertThatThrownBy(() -> jev.evaluate(Review.class, "ok")).rootCause()
                .hasMessageContaining("'mentions_shipping'");
    }
}
