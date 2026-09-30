package io.github.rbrisuda.jevface;

import io.github.rbrisuda.jevface.spi.NoulSpec;
import io.github.rbrisuda.jevface.spi.QuestionSpec;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One question of an agent.
 *
 * @param key question id sent to Jev
 * @param method the interface method
 * @param spec what is sent to Jev
 * @param statedKey id of the optional yes/no gate, or {@code null}
 * @param statedSpec the optional yes/no gate, or {@code null}
 * @param minConfidence effective confidence floor
 * @param options Jev label to Java value (enum constant or {@code String}), in order; empty for nouls
 */
public record QuestionDescriptor(
        String key,
        Method method,
        QuestionSpec spec,
        @Nullable String statedKey,
        @Nullable NoulSpec statedSpec,
        double minConfidence,
        Map<String, Object> options) {

    public QuestionDescriptor {
        options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    /** The Jev label of a Java option value, e.g. {@code Department.BILLING -> "billing"}. */
    public String labelOf(Object value) {
        for (Map.Entry<String, Object> option : options.entrySet()) {
            if (Objects.equals(option.getValue(), value)) {
                return option.getKey();
            }
        }
        throw new IllegalArgumentException(value + " is not an option of question '" + key + "': " + options.keySet());
    }
}
