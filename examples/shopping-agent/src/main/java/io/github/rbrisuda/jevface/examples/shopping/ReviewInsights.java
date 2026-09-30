package io.github.rbrisuda.jevface.examples.shopping;

import io.github.rbrisuda.jevface.JevClient;
import io.github.rbrisuda.jevface.Jevface;

import java.util.*;

/**
 * Aggregates many reviews. All Jev calls happen in one batch; the rest is ordinary Java.
 */
public final class ReviewInsights {

    public record Report(
            int reviews,
            int flagged,
            double averageStars,
            double buyAgainRate,
            Map<Topic, Integer> topics,
            Map<String, Integer> sizeIssues) {
    }

    private final JevClient<ProductReview> reviews;

    public ReviewInsights(Jevface jev) {
        this.reviews = jev.client(ProductReview.class);
    }

    public Report analyze(List<String> texts) {
        List<ProductReview> genuine = reviews.evaluateAll(texts).stream()
                .filter(review -> !review.suspicious())
                .toList();

        Map<Topic, Integer> topics = new EnumMap<>(Topic.class);
        Map<String, Integer> sizeIssues = new TreeMap<>();
        for (ProductReview review : genuine) {
            topics.merge(review.topic(), 1, Integer::sum);
            review.sizeIssue().ifPresent(issue -> sizeIssues.merge(issue, 1, Integer::sum));
        }
        double averageStars = genuine.stream().mapToInt(ProductReview::stars).average().orElse(0);
        double buyAgain = genuine.stream().mapToDouble(review -> review.wouldBuyAgain().isTrue() ? 1 : 0).average().orElse(0);
        return new Report(texts.size(), texts.size() - genuine.size(), averageStars, buyAgain, topics, sizeIssues);
    }

    static Optional<String> describe(Report report) {
        if (report.reviews() == 0) {
            return Optional.empty();
        }
        return Optional.of(String.format(Locale.ROOT, """
                        Reviews analysed: %d (%d flagged as suspicious and ignored)
                        Average rating:   %.1f stars
                        Would buy again:  %.0f%%
                        Topics:           %s
                        Size issues:      %s""",
                report.reviews(), report.flagged(), report.averageStars(), report.buyAgainRate() * 100,
                report.topics(), report.sizeIssues()));
    }
}
