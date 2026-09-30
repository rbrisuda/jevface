package io.github.rbrisuda.jevface.spi;

import org.jspecify.annotations.Nullable;

/** Yes/no question with optional descriptions of what yes and no mean. */
public record NoulSpec(String instructions, @Nullable String whenTrue, @Nullable String whenFalse)
        implements QuestionSpec {

    public static NoulSpec of(String instructions) {
        return new NoulSpec(instructions, null, null);
    }
}
