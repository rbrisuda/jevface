package io.github.rbrisuda.jevface.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The chosen option label, the probability per label, and the confidence. */
public record ChoiceAnswer(String choice, Map<String, Double> probabilities, double confidence) implements Answer {

    public ChoiceAnswer {
        probabilities = Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
    }
}
