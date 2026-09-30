package io.github.rbrisuda.jevface.spring.other;

import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;

@JevAgent
public interface Spam {

    @NoulQuestion("Is this message spam?")
    boolean spam();

    /** Not an agent: must not get a client. */
    interface Helper {}
}
