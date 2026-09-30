package io.github.rbrisuda.jevface;

import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.TokenUsage;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Details of the Jev call behind an evaluated agent.
 *
 * @param agentName the agent's name
 * @param state the prompt that was judged
 * @param model the model that answered, if reported
 * @param requestId the backend request id, if reported
 * @param usage token usage
 * @param latency wall-clock time of the call; for batches, of the whole batch
 * @param answers raw answers by question id
 */
public record EvaluationMetadata(
        String agentName,
        String state,
        @Nullable String model,
        @Nullable String requestId,
        TokenUsage usage,
        Duration latency,
        Map<String, Answer> answers) {

    public EvaluationMetadata {
        answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }
}
