package io.github.rbrisuda.jevface.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The engine's answers to a {@link JudgmentRequest}.
 *
 * @param answers answers by question id
 * @param model the model that answered, if reported
 * @param requestId the backend's request id, if reported
 * @param usage token usage
 */
public record Judgment(
        Map<String, Answer> answers, @Nullable String model, @Nullable String requestId, TokenUsage usage) {

    public Judgment {
        answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }
}
