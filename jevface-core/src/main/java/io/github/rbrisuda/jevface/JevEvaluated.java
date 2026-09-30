package io.github.rbrisuda.jevface;

/**
 * Implemented by every evaluated agent. Extend it in your agent interface to reach the metadata
 * without a cast, or use {@link Jevface#evaluationOf(Object)}.
 */
public interface JevEvaluated {

    /** Details of the Jev call that produced this result. */
    EvaluationMetadata evaluation();
}
