package io.github.rbrisuda.jevface.spi;

import java.util.List;

/** Rate the state on ordered {@link #levels()}, lowest first. */
public record ScoreSpec(String instructions, List<String> levels) implements QuestionSpec {

    public ScoreSpec {
        levels = List.copyOf(levels);
    }
}
