package io.github.rbrisuda.jevface.internal;

import io.github.rbrisuda.jevface.AgentDescriptor;
import java.util.List;

/**
 * An agent interface compiled into what is sent to Jev ({@link #descriptor()}) and how each answer
 * becomes a Java value ({@link #bindings()}). Internal API.
 */
public record AgentModel<T>(Class<T> type, AgentDescriptor descriptor, List<QuestionBinding> bindings) {

    public AgentModel {
        bindings = List.copyOf(bindings);
    }
}
