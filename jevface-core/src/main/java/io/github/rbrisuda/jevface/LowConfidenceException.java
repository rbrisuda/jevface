package io.github.rbrisuda.jevface;

import java.util.Locale;

/**
 * A question method with a plain return type was called, but Jev's confidence was below the
 * question's {@code minConfidence}. Declare the method as {@code Optional<...>} to get an empty value
 * instead, or as {@code ChoiceResult}/{@code ScoreResult} to inspect the confidence yourself.
 */
public class LowConfidenceException extends JevException {

    private final String questionKey;
    private final double confidence;
    private final double minConfidence;

    public LowConfidenceException(String questionKey, double confidence, double minConfidence) {
        super(String.format(Locale.ROOT, "Confidence %.2f for question '%s' is below the required %.2f",
                confidence, questionKey, minConfidence));
        this.questionKey = questionKey;
        this.confidence = confidence;
        this.minConfidence = minConfidence;
    }

    public String questionKey() {
        return questionKey;
    }

    public double confidence() {
        return confidence;
    }

    public double minConfidence() {
        return minConfidence;
    }
}
