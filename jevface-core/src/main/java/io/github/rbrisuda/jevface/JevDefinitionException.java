package io.github.rbrisuda.jevface;

import java.util.List;

/**
 * An agent interface is not a valid {@link io.github.rbrisuda.jevface.annotation.JevAgent}. Thrown when a client
 * is created, so mistakes surface at startup rather than at the first request. The message lists
 * every problem found.
 */
public class JevDefinitionException extends JevException {

    private final List<String> problems;

    public JevDefinitionException(Class<?> agentType, List<String> problems) {
        super("Invalid @JevAgent " + agentType.getName() + ":\n - " + String.join("\n - ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
