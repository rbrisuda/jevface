package io.github.rbrisuda.jevface;

import org.jspecify.annotations.Nullable;

/** The Jev call failed, or the engine returned answers that do not match the questions asked. */
public class JevEvaluationException extends JevException {

    public JevEvaluationException(String message) {
        super(message);
    }

    public JevEvaluationException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
