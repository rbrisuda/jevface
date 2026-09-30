package io.github.rbrisuda.jevface.internal;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * What a question method returns: a value, or an exception thrown each time the method is called.
 * Failures are deferred so that an agent can be evaluated even if one answer is not usable, and only
 * code that actually reads that answer fails.
 */
public sealed interface Outcome {

    @Nullable Object get();

    static Outcome of(@Nullable Object value) {
        return new Present(value);
    }

    static Outcome failure(Supplier<? extends RuntimeException> exception) {
        return new Failed(exception);
    }

    record Present(@Nullable Object value) implements Outcome {
        @Override
        public @Nullable Object get() {
            return value;
        }
    }

    record Failed(Supplier<? extends RuntimeException> exception) implements Outcome {
        @Override
        public @Nullable Object get() {
            throw exception.get();
        }
    }
}
