package io.github.rbrisuda.jevface.spi;

/** Token usage of one Jev call. */
public record TokenUsage(int inputTokens, int outputTokens) {

    public static final TokenUsage EMPTY = new TokenUsage(0, 0);

    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
