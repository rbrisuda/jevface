package io.github.rbrisuda.jevface.internal;

import io.github.rbrisuda.jevface.AgentDescriptor;
import io.github.rbrisuda.jevface.EvaluationMetadata;
import io.github.rbrisuda.jevface.JevEvaluated;
import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.ChoiceAnswer;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import io.github.rbrisuda.jevface.spi.ScoreAnswer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.jspecify.annotations.Nullable;

/**
 * Backs an evaluated agent: question methods return pre-computed outcomes, default methods run
 * their own code, and {@code evaluation()} returns the call metadata.
 */
final class ResultInvocationHandler implements InvocationHandler {

    private final AgentDescriptor descriptor;
    private final Map<Method, Outcome> outcomes;
    private final EvaluationMetadata metadata;

    ResultInvocationHandler(AgentDescriptor descriptor, Map<Method, Outcome> outcomes, EvaluationMetadata metadata) {
        this.descriptor = descriptor;
        this.outcomes = Map.copyOf(outcomes);
        this.metadata = metadata;
    }

    @Override
    @SuppressWarnings("ReferenceEquality") // evaluated agents have identity semantics
    public @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
        if (AgentIntrospector.isObjectMethod(method)) {
            return switch (method.getName()) {
                case "equals" -> args != null && proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> render();
            };
        }
        if (method.getDeclaringClass() == JevEvaluated.class) {
            return metadata;
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        Outcome outcome = outcomes.get(method);
        if (outcome == null) {
            throw new IllegalStateException(method + " is not a question method of " + descriptor.type().getName());
        }
        return outcome.get();
    }

    private String render() {
        StringJoiner answers = new StringJoiner(", ", descriptor.name() + "{", "}");
        metadata.answers().forEach((key, answer) -> answers.add(key + "=" + render(answer)));
        return answers.toString();
    }

    private static String render(Answer answer) {
        return switch (answer) {
            case NoulAnswer noul -> String.format(Locale.ROOT, "%.2f", noul.value());
            case ChoiceAnswer choice -> String.format(Locale.ROOT, "%s (confidence %.2f)", choice.choice(), choice.confidence());
            case ScoreAnswer score -> String.format(Locale.ROOT, "%.2f (confidence %.2f)", score.score(), score.confidence());
        };
    }
}
