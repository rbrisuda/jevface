package io.github.rbrisuda.jevface.spi;

/** An engine's answer to one {@link QuestionSpec}. */
public sealed interface Answer permits NoulAnswer, ChoiceAnswer, ScoreAnswer {
}
