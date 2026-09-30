package io.github.rbrisuda.jevface.internal;

import io.github.rbrisuda.jevface.QuestionDescriptor;
import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import org.jspecify.annotations.Nullable;

/** A question together with the function that turns Jev's answer into the method's return value. */
public record QuestionBinding(QuestionDescriptor descriptor, AnswerMapper mapper) {

    /** Maps an answer (and the answer of the optional {@code stated} gate) to a method outcome. */
    @FunctionalInterface
    public interface AnswerMapper {
        Outcome map(Answer answer, @Nullable NoulAnswer stated);
    }
}
