package io.github.rbrisuda.jevface.spi;

/** Probability, between 0 and 1, that the answer to a yes/no question is yes. */
public record NoulAnswer(double value) implements Answer {
}
