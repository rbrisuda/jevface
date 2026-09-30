package io.github.rbrisuda.jevface;

import org.jspecify.annotations.Nullable;

/** Base class of all jevface exceptions. */
public class JevException extends RuntimeException {

    public JevException(String message) {
        super(message);
    }

    public JevException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
