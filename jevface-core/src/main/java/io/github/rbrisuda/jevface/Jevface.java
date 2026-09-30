package io.github.rbrisuda.jevface;

import io.github.rbrisuda.jevface.internal.AgentIntrospector;
import io.github.rbrisuda.jevface.internal.DefaultJevClient;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Entry point: turns {@link io.github.rbrisuda.jevface.annotation.JevAgent} interfaces into Jev clients.
 *
 * <pre>{@code
 * Jevface jev = Jevface.create(new TypeSafeJudgmentEngine(TypeSafeClient.builder().build()));
 * SupportTriage triage = jev.evaluate(SupportTriage.class, "My payouts have failed for 3 days!");
 * if (triage.isUrgent()) { ... }
 * }</pre>
 */
public final class Jevface {

    private final JudgmentEngine engine;
    private final ConcurrentMap<Class<?>, JevClient<?>> clients = new ConcurrentHashMap<>();

    private Jevface(JudgmentEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public static Jevface create(JudgmentEngine engine) {
        return new Jevface(engine);
    }

    /**
     * Returns the client for an agent interface, validating the interface on first use.
     *
     * @throws JevDefinitionException if the interface is not a valid agent
     */
    @SuppressWarnings("unchecked")
    public <T> JevClient<T> client(Class<T> agentType) {
        return (JevClient<T>) clients.computeIfAbsent(agentType, this::newClient);
    }

    /** Shorthand for {@code client(agentType).evaluate(prompt)}. */
    public <T> T evaluate(Class<T> agentType, String prompt) {
        return client(agentType).evaluate(prompt);
    }

    /** Validates an agent interface and describes the questions it sends, without calling Jev. */
    public static AgentDescriptor describe(Class<?> agentType) {
        return AgentIntrospector.introspect(agentType).descriptor();
    }

    /** The Jev call details behind an evaluated agent instance. */
    public static EvaluationMetadata evaluationOf(Object evaluatedAgent) {
        if (evaluatedAgent instanceof JevEvaluated evaluated) {
            return evaluated.evaluation();
        }
        throw new IllegalArgumentException(evaluatedAgent.getClass().getName() + " was not produced by Jevface");
    }

    private <T> JevClient<T> newClient(Class<T> agentType) {
        return new DefaultJevClient<>(AgentIntrospector.introspect(agentType), engine);
    }
}
