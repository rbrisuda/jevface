package io.github.rbrisuda.jevface.spi;

/** A typed question, independent of any SDK. */
public sealed interface QuestionSpec permits NoulSpec, ChoiceSpec, ScoreSpec {

    /** The question text. */
    String instructions();
}
