package io.github.rbrisuda.jevface;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Answer to a {@link io.github.rbrisuda.jevface.annotation.ScoreQuestion}.
 *
 * @param value continuous, probability-weighted level; 0 is the first level and 1.1 lies just past the second
 * @param level the level nearest to {@link #value()}
 * @param levelIndex index of {@link #level()}
 * @param confidence how cleanly the levels separated for this input, between 0 and 1
 * @param probabilities probability per level, lowest level first; they sum to 1
 * @param <T> the level type: an enum or {@code String}
 */
public record ScoreResult<T>(double value, T level, int levelIndex, double confidence, Map<T, Double> probabilities) {

    public ScoreResult {
        probabilities = Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
    }

    public double probabilityOf(T level) {
        Double probability = probabilities.get(level);
        return probability != null ? probability : 0.0;
    }
}
