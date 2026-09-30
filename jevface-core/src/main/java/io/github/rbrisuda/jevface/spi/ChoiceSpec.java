package io.github.rbrisuda.jevface.spi;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Pick one of {@link #options()}. */
public record ChoiceSpec(String instructions, List<Option> options) implements QuestionSpec {

    public ChoiceSpec {
        options = List.copyOf(options);
    }

    /** An option label and an optional description of what it means. */
    public record Option(String label, @Nullable String description) {
    }
}
