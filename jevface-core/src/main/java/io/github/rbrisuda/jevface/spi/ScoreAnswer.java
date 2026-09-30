package io.github.rbrisuda.jevface.spi;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** The probability-weighted level value, the probability per level index, and the confidence. */
public record ScoreAnswer(double score, Map<Integer, Double> probabilities, double confidence) implements Answer {

    public ScoreAnswer {
        probabilities = Collections.unmodifiableMap(new TreeMap<>(probabilities));
    }
}
