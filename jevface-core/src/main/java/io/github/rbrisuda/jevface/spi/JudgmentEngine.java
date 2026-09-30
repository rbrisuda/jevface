package io.github.rbrisuda.jevface.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * The only thing the core needs from a Jev backend: answer typed questions about a state.
 *
 * <p>The default implementation, {@code TypeSafeJudgmentEngine}, calls TypeSafe's Jev API. Any
 * backend speaking the same protocol (e.g. a local Laya server) or a test stub works too.
 */
public interface JudgmentEngine {

    /** Answers every question of the request against its state, in one call. */
    Judgment judge(JudgmentRequest request);

    /**
     * Answers several independent requests. Engines that support batching should override this; the
     * default calls {@link #judge(JudgmentRequest)} sequentially. Results keep the request order.
     */
    default List<Judgment> judgeAll(List<JudgmentRequest> requests) {
        List<Judgment> judgments = new ArrayList<>(requests.size());
        for (JudgmentRequest request : requests) {
            judgments.add(judge(request));
        }
        return judgments;
    }
}
