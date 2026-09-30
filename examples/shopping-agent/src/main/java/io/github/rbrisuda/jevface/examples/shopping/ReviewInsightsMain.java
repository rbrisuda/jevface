package io.github.rbrisuda.jevface.examples.shopping;

import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.typesafe.TypeSafeJudgmentEngine;
import java.util.List;

/**
 * {@code TYPESAFE_API_KEY=... ./gradlew :examples:shopping-agent:run [--args="'review 1' 'review 2'"]}
 *
 * <p>Set {@code TYPESAFE_BASE_URL} to use a local Laya server instead of the hosted API.
 */
public final class ReviewInsightsMain {

    static final List<String> SAMPLE_REVIEWS = List.of(
            "Lovely jacket, warm and well stitched. Runs a size small though, order one up.",
            "Arrived two weeks late and the box was crushed. Never ordering here again.",
            "BEST SHOP EVER!!! 100% recommend!!! Visit my site for discount codes!!!",
            "Good value for the price. Customer service swapped my size without any fuss.",
            "The shoes are huge, at least two sizes too large. Returning them.");

    private ReviewInsightsMain() {}

    public static void main(String[] args) {
        String key = System.getenv("TYPESAFE_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("Set TYPESAFE_API_KEY (and optionally TYPESAFE_BASE_URL for a local Laya server).");
            System.exit(1);
        }
        List<String> reviews = args.length > 0 ? List.of(args) : SAMPLE_REVIEWS;

        Jevface jev = Jevface.create(TypeSafeJudgmentEngine.fromEnvironment());
        ReviewInsights.describe(new ReviewInsights(jev).analyze(reviews)).ifPresent(System.out::println);
    }
}
