package io.github.rbrisuda.jevface.spring.app;

import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;

@JevAgent
public interface Mood {

    @NoulQuestion("Is the writer happy?")
    boolean happy();
}
