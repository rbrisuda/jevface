package io.github.rbrisuda.jevface.examples.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.test.AgentStub;
import io.github.rbrisuda.jevface.test.StubJudgmentEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ReviewInsightsTest {

    /** One stub engine per review text; the lambda shows how small the JudgmentEngine SPI is. */
    private static JudgmentEngine perReview(Map<String, Consumer<AgentStub<ProductReview>>> answers) {
        Map<String, StubJudgmentEngine> engines = new HashMap<>();
        answers.forEach((text, stubbing) -> {
            StubJudgmentEngine engine = new StubJudgmentEngine();
            stubbing.accept(engine.forAgent(ProductReview.class));
            engines.put(text, engine);
        });
        return request -> engines.get(request.state()).judge(request);
    }

    private static Consumer<AgentStub<ProductReview>> review(int stars, Topic topic, boolean buyAgain, double spam) {
        return stub -> stub
                .score(ProductReview::ratingIndex, stars - 1)
                .choice(ProductReview::topic, topic)
                .noul(ProductReview::suspicious, spam)
                .noul(ProductReview::wouldBuyAgain, buyAgain ? 0.9 : 0.1)
                .choice(ProductReview::sizeIssue, "too-small")
                .stated(ProductReview::sizeIssue, topic == Topic.SIZE_FIT ? 0.9 : 0.1);
    }

    @Test
    void aggregatesGenuineReviews() {
        Jevface jev = Jevface.create(perReview(Map.of(
                "a", review(5, Topic.SIZE_FIT, true, 0.1),
                "b", review(1, Topic.DELIVERY, false, 0.2),
                "c", review(5, Topic.PRICE, true, 0.95),
                "d", review(4, Topic.SIZE_FIT, true, 0.6))));

        ReviewInsights.Report report = new ReviewInsights(jev).analyze(List.of("a", "b", "c", "d"));

        assertThat(report.reviews()).isEqualTo(4);
        assertThat(report.flagged()).as("0.6 is below the 0.7 threshold").isEqualTo(1);
        assertThat(report.averageStars()).isEqualTo((5 + 1 + 4) / 3.0);
        assertThat(report.buyAgainRate()).isEqualTo(2 / 3.0);
        assertThat(report.topics()).containsExactly(Map.entry(Topic.SIZE_FIT, 2), Map.entry(Topic.DELIVERY, 1));
        assertThat(report.sizeIssues()).containsExactly(Map.entry("too-small", 2));
        assertThat(ReviewInsights.describe(report)).get().asString().contains("Average rating:   3.3 stars");
    }

    @Test
    void sampleReviewsNeedNoKeyToCompile() {
        assertThat(ReviewInsightsMain.SAMPLE_REVIEWS).hasSize(5);
        assertThat(Jevface.describe(ProductReview.class).questionSpecs())
                .containsOnlyKeys("rating", "topic", "suspicious", "would_buy_again", "size_issue", "size_issue_stated");
    }
}
