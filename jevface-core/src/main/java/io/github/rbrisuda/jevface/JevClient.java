package io.github.rbrisuda.jevface;

import java.util.List;

/**
 * Evaluates prompts against one {@link io.github.rbrisuda.jevface.annotation.JevAgent} interface. Thread-safe
 * and reusable; obtain one from {@link Jevface#client(Class)}.
 *
 * @param <T> the agent interface
 */
public interface JevClient<T> {

    Class<T> agentType();

    /** The validated question model of the agent, including what is sent to Jev. */
    AgentDescriptor descriptor();

    /**
     * Asks every question of the agent about {@code prompt} in a single Jev call and returns the
     * answers as an instance of the agent interface.
     */
    T evaluate(String prompt);

    /** Evaluates several independent prompts, batched where the engine supports it. Keeps the order. */
    List<T> evaluateAll(List<String> prompts);
}
