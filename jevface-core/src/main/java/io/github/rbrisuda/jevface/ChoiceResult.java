package io.github.rbrisuda.jevface;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Answer to a {@link io.github.rbrisuda.jevface.annotation.ChoiceQuestion}.
 *
 * @param value the most probable option
 * @param confidence how cleanly the options separated for this input, between 0 and 1
 * @param probabilities probability per option, in declaration order; they sum to 1
 * @param <T> the option type: an enum or {@code String}
 */
public record ChoiceResult<T>(T value, double confidence, Map<T, Double> probabilities) {

    public ChoiceResult {
        probabilities = Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
    }

    public double probabilityOf(T option) {
        Double probability = probabilities.get(option);
        return probability != null ? probability : 0.0;
    }
}
