package io.github.rbrisuda.jevface;

import io.github.rbrisuda.jevface.spi.NoulSpec;
import io.github.rbrisuda.jevface.spi.QuestionSpec;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The validated question model of a {@link io.github.rbrisuda.jevface.annotation.JevAgent} interface.
 *
 * @param type the agent interface
 * @param name agent name
 * @param description agent description
 * @param model Jev model, or {@code null} for the engine default
 * @param questions questions ordered by key
 */
public record AgentDescriptor(
        Class<?> type, String name, String description, @Nullable String model, List<QuestionDescriptor> questions) {

    public AgentDescriptor {
        questions = List.copyOf(questions);
    }

    /** Every question sent to Jev, by id, including the yes/no gates of {@code stated} questions. */
    public Map<String, QuestionSpec> questionSpecs() {
        Map<String, QuestionSpec> specs = new LinkedHashMap<>();
        for (QuestionDescriptor question : questions) {
            specs.put(question.key(), question.spec());
            String statedKey = question.statedKey();
            NoulSpec statedSpec = question.statedSpec();
            if (statedKey != null && statedSpec != null) {
                specs.put(statedKey, statedSpec);
            }
        }
        return Collections.unmodifiableMap(specs);
    }

    public QuestionDescriptor question(Method method) {
        for (QuestionDescriptor question : questions) {
            if (question.method().equals(method)) {
                return question;
            }
        }
        throw new IllegalArgumentException(method + " is not a question method of " + type.getName());
    }

    public QuestionDescriptor question(String key) {
        for (QuestionDescriptor question : questions) {
            if (question.key().equals(key)) {
                return question;
            }
        }
        throw new IllegalArgumentException("No question '" + key + "' in " + type.getName());
    }
}
