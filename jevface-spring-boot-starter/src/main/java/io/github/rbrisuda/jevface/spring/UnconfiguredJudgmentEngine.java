package io.github.rbrisuda.jevface.spring;

import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.spi.Judgment;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.spi.JudgmentRequest;

/** Placeholder so applications start without an API key; any evaluation explains what to configure. */
final class UnconfiguredJudgmentEngine implements JudgmentEngine {

    static final String MESSAGE = "No Jev engine configured. Set the TYPESAFE_API_KEY environment variable "
            + "(optionally TYPESAFE_BASE_URL, e.g. for a local Laya server) or spring.ai.typesafe.api-key, "
            + "or define a JudgmentEngine bean.";

    @Override
    public Judgment judge(JudgmentRequest request) {
        throw new JevEvaluationException(MESSAGE + " (while evaluating " + request.agentName() + ")");
    }

    @Override
    public String toString() {
        return "UnconfiguredJudgmentEngine";
    }
}
