package io.github.rbrisuda.jevface.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One Jev call: a state (the prompt) and the typed questions to answer about it.
 *
 * @param agentName name of the agent the questions come from; not sent to Jev
 * @param state the input being judged
 * @param model the Jev model, or {@code null} for the engine's default
 * @param questions questions by id, in a stable order
 */
public record JudgmentRequest(
        String agentName, String state, @Nullable String model, Map<String, QuestionSpec> questions) {

    public JudgmentRequest {
        Objects.requireNonNull(agentName, "agentName");
        Objects.requireNonNull(state, "state");
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("A judgment request needs at least one question");
        }
        questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
    }
}
