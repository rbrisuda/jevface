package io.github.rbrisuda.jevface.test;

import io.github.rbrisuda.jevface.AgentDescriptor;
import io.github.rbrisuda.jevface.QuestionDescriptor;
import io.github.rbrisuda.jevface.spi.ChoiceAnswer;
import io.github.rbrisuda.jevface.spi.ChoiceSpec;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import io.github.rbrisuda.jevface.spi.NoulSpec;
import io.github.rbrisuda.jevface.spi.QuestionSpec;
import io.github.rbrisuda.jevface.spi.ScoreAnswer;
import io.github.rbrisuda.jevface.spi.ScoreSpec;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Stubs the answers of one agent. Questions are referenced with method references, so renaming a
 * method or changing its annotation keeps tests in sync or breaks them at compile/stub time.
 *
 * @param <T> the agent interface
 */
public final class AgentStub<T> {

    private final StubJudgmentEngine engine;
    private final Class<T> agentType;
    private final AgentDescriptor descriptor;

    AgentStub(StubJudgmentEngine engine, Class<T> agentType, AgentDescriptor descriptor) {
        this.engine = engine;
        this.agentType = agentType;
        this.descriptor = descriptor;
        for (QuestionDescriptor question : descriptor.questions()) {
            String statedKey = question.statedKey();
            if (statedKey != null) {
                engine.putIfAbsent(descriptor.name(), statedKey, new NoulAnswer(1.0));
            }
        }
    }

    /** Answers a {@code @NoulQuestion} with the probability of yes. */
    public AgentStub<T> noul(Function<? super T, ?> question, double value) {
        QuestionDescriptor descriptor = question(question, NoulSpec.class);
        engine.put(this.descriptor.name(), descriptor.key(), new NoulAnswer(unit(value)));
        return this;
    }

    /** Answers a {@code @ChoiceQuestion} with full confidence. */
    public AgentStub<T> choice(Function<? super T, ?> question, Object option) {
        return choice(question, option, 1.0);
    }

    /**
     * Answers a {@code @ChoiceQuestion}. {@code option} is the enum constant or String option. The
     * chosen option gets probability {@code confidence}; the rest is spread over the other options.
     */
    public AgentStub<T> choice(Function<? super T, ?> question, Object option, double confidence) {
        QuestionDescriptor descriptor = question(question, ChoiceSpec.class);
        String chosen = label(descriptor, option);
        double rest = descriptor.options().size() > 1 ? (1.0 - unit(confidence)) / (descriptor.options().size() - 1) : 0;
        Map<String, Double> probabilities = new LinkedHashMap<>();
        descriptor.options().keySet().forEach(label -> probabilities.put(label, label.equals(chosen) ? confidence : rest));
        engine.put(this.descriptor.name(), descriptor.key(), new ChoiceAnswer(chosen, probabilities, confidence));
        return this;
    }

    /** Answers a {@code @ScoreQuestion} with full confidence. */
    public AgentStub<T> score(Function<? super T, ?> question, double score) {
        return score(question, score, 1.0);
    }

    /** Answers a {@code @ScoreQuestion} with a continuous level value (0 = first level). */
    public AgentStub<T> score(Function<? super T, ?> question, double score, double confidence) {
        QuestionDescriptor descriptor = question(question, ScoreSpec.class);
        int levels = descriptor.options().size();
        if (score < 0 || score > levels - 1) {
            throw new IllegalArgumentException("score must be between 0 and " + (levels - 1) + " for '" + descriptor.key() + "'");
        }
        int nearest = (int) Math.round(score);
        Map<Integer, Double> probabilities = new LinkedHashMap<>();
        for (int i = 0; i < levels; i++) {
            probabilities.put(i, i == nearest ? 1.0 : 0.0);
        }
        engine.put(this.descriptor.name(), descriptor.key(), new ScoreAnswer(score, probabilities, unit(confidence)));
        return this;
    }

    /** Answers a {@code @ScoreQuestion} exactly at a level (enum constant or level text). */
    public AgentStub<T> scoreLevel(Function<? super T, ?> question, Object level, double confidence) {
        QuestionDescriptor descriptor = question(question, ScoreSpec.class);
        List<String> labels = List.copyOf(descriptor.options().keySet());
        return score(question, labels.indexOf(label(descriptor, level)), confidence);
    }

    /** Answers the {@code stated} gate of a Choice or Score question. Below its threshold the answer is absent. */
    public AgentStub<T> stated(Function<? super T, ?> question, double value) {
        QuestionDescriptor descriptor = descriptor(question);
        String statedKey = descriptor.statedKey();
        if (statedKey == null) {
            throw new IllegalArgumentException(descriptor.method().getName() + "() has no stated(...) gate");
        }
        engine.put(this.descriptor.name(), statedKey, new NoulAnswer(unit(value)));
        return this;
    }

    public StubJudgmentEngine engine() {
        return engine;
    }

    private QuestionDescriptor question(Function<? super T, ?> question, Class<? extends QuestionSpec> expected) {
        QuestionDescriptor descriptor = descriptor(question);
        if (!expected.isInstance(descriptor.spec())) {
            throw new IllegalArgumentException(descriptor.method().getName() + "() is a "
                    + descriptor.spec().getClass().getSimpleName().replace("Spec", "") + " question, not a "
                    + expected.getSimpleName().replace("Spec", ""));
        }
        return descriptor;
    }

    private QuestionDescriptor descriptor(Function<? super T, ?> question) {
        Method method = MethodRecorder.capture(agentType, question);
        return descriptor.question(method);
    }

    private static String label(QuestionDescriptor descriptor, Object option) {
        if (option instanceof String label && descriptor.options().containsKey(label)) {
            return label;
        }
        return descriptor.labelOf(option);
    }

    private static double unit(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("value must be between 0 and 1, was " + value);
        }
        return value;
    }
}
