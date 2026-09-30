package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.annotation.Option;

/** Score levels, ordered from lowest to highest. The description is the level text Jev sees. */
public enum Frustration {
    @Option(description = "Calm, neutral or friendly")
    CALM,
    @Option(description = "Annoyed or impatient")
    ANNOYED,
    @Option(description = "Angry, threatening to leave or escalate")
    ANGRY
}
