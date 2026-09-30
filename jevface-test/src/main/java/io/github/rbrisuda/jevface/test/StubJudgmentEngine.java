package io.github.rbrisuda.jevface.test;

import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.Judgment;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.spi.JudgmentRequest;
import io.github.rbrisuda.jevface.spi.TokenUsage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link JudgmentEngine} that answers from stubbed values instead of calling Jev.
 *
 * <pre>{@code
 * StubJudgmentEngine engine = new StubJudgmentEngine();
 * engine.forAgent(SupportTriage.class)
 *       .noul(SupportTriage::isUrgent, 0.95)
 *       .choice(SupportTriage::department, Department.BILLING, 0.82);
 *
 * SupportTriage triage = Jevface.create(engine).evaluate(SupportTriage.class, "any prompt");
 * }</pre>
 *
 * Every question must be stubbed; unanswered questions fail with a message naming the method.
 * {@code stated} gates default to 1.0 (the answer is present). Thread-safe.
 */
public final class StubJudgmentEngine implements JudgmentEngine {

    private final Map<String, Map<String, Answer>> answersByAgent = new ConcurrentHashMap<>();
    private final List<JudgmentRequest> requests = new CopyOnWriteArrayList<>();

    /** Start stubbing the answers of one agent interface. */
    public <T> AgentStub<T> forAgent(Class<T> agentType) {
        return new AgentStub<>(this, agentType, Jevface.describe(agentType));
    }

    /** Every request received so far, e.g. to assert on the prompt or the questions. */
    public List<JudgmentRequest> requests() {
        return List.copyOf(requests);
    }

    /** Forgets all stubbed answers and recorded requests. */
    public void reset() {
        answersByAgent.clear();
        requests.clear();
    }

    @Override
    public Judgment judge(JudgmentRequest request) {
        requests.add(request);
        Map<String, Answer> stubbed = answersByAgent.getOrDefault(request.agentName(), Map.of());
        Map<String, Answer> answers = new LinkedHashMap<>();
        for (String key : request.questions().keySet()) {
            Answer answer = stubbed.get(key);
            if (answer == null) {
                throw new IllegalStateException("No stubbed answer for question '" + key + "' of agent "
                        + request.agentName() + ". Stub it via forAgent(...).noul/choice/score(...)");
            }
            answers.put(key, answer);
        }
        return new Judgment(answers, "stub", null, TokenUsage.EMPTY);
    }

    void put(String agentName, String key, Answer answer) {
        answersByAgent.computeIfAbsent(agentName, name -> new ConcurrentHashMap<>()).put(key, answer);
    }

    void putIfAbsent(String agentName, String key, Answer answer) {
        answersByAgent.computeIfAbsent(agentName, name -> new ConcurrentHashMap<>()).putIfAbsent(key, answer);
    }
}
