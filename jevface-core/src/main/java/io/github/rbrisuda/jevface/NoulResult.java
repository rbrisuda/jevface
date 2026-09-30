package io.github.rbrisuda.jevface;

/**
 * Answer to a {@link io.github.rbrisuda.jevface.annotation.NoulQuestion}.
 *
 * @param value probability, between 0 and 1, that the answer is yes; 0.5 means undecided
 * @param threshold the question's threshold
 */
public record NoulResult(double value, double threshold) {

    /** Whether {@link #value()} reaches the {@link #threshold()}. */
    public boolean isTrue() {
        return value >= threshold;
    }
}
