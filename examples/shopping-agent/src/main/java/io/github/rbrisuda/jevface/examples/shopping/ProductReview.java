package io.github.rbrisuda.jevface.examples.shopping;

import io.github.rbrisuda.jevface.NoulResult;
import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.Option;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import java.util.Optional;

/** One product review, judged by Jev. Implemented by jevface at runtime. */
@JevAgent(name = "product-review")
public interface ProductReview {

    /** Nearest level index: 0 = terrible ... 4 = excellent. */
    @ScoreQuestion(value = "How satisfied is the reviewer with the product?",
            levels = {"Terrible", "Poor", "Okay", "Good", "Excellent"}, key = "rating")
    int ratingIndex();

    @ChoiceQuestion("What is the review mainly about?")
    Topic topic();

    /** A higher threshold than the default 0.5: only flag when Jev is fairly sure. */
    @NoulQuestion(value = "Does the review look fake, paid or spam?", threshold = 0.7)
    boolean suspicious();

    @NoulQuestion(value = "Would the reviewer buy from this shop again?",
            whenTrue = "Says so, or is clearly satisfied", whenFalse = "Says they would not, or is clearly disappointed")
    NoulResult wouldBuyAgain();

    /** String options instead of an enum; empty when the review does not talk about fit. */
    @ChoiceQuestion(value = "What is the size problem?", stated = "Does the review mention a size or fit problem?",
            options = {@Option(label = "too-small"), @Option(label = "too-large"), @Option(label = "inconsistent")})
    Optional<String> sizeIssue();

    default int stars() {
        return ratingIndex() + 1;
    }
}
