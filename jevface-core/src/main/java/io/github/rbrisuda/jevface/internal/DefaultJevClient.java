package io.github.rbrisuda.jevface.internal;

import io.github.rbrisuda.jevface.AgentDescriptor;
import io.github.rbrisuda.jevface.EvaluationMetadata;
import io.github.rbrisuda.jevface.JevClient;
import io.github.rbrisuda.jevface.JevEvaluated;
import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.JevException;
import io.github.rbrisuda.jevface.QuestionDescriptor;
import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.Judgment;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.spi.JudgmentRequest;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** Sends an agent's questions to a {@link JudgmentEngine} and wraps the answers in a proxy. Internal API. */
public final class DefaultJevClient<T> implements JevClient<T> {

    private final AgentModel<T> model;
    private final JudgmentEngine engine;
    private final Class<?>[] proxyInterfaces;

    public DefaultJevClient(AgentModel<T> model, JudgmentEngine engine) {
        this.model = Objects.requireNonNull(model, "model");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.proxyInterfaces = JevEvaluated.class.isAssignableFrom(model.type())
                ? new Class<?>[] {model.type()}
                : new Class<?>[] {model.type(), JevEvaluated.class};
    }

    @Override
    public Class<T> agentType() {
        return model.type();
    }

    @Override
    public AgentDescriptor descriptor() {
        return model.descriptor();
    }

    @Override
    public T evaluate(String prompt) {
        JudgmentRequest request = request(prompt);
        long start = System.nanoTime();
        Judgment judgment = call(() -> engine.judge(request));
        return toAgent(prompt, judgment, Duration.ofNanos(System.nanoTime() - start));
    }

    @Override
    public List<T> evaluateAll(List<String> prompts) {
        if (prompts.isEmpty()) {
            return List.of();
        }
        List<JudgmentRequest> requests = prompts.stream().map(this::request).toList();
        long start = System.nanoTime();
        List<Judgment> judgments = call(() -> engine.judgeAll(requests));
        Duration latency = Duration.ofNanos(System.nanoTime() - start);
        if (judgments.size() != prompts.size()) {
            throw new JevEvaluationException("Engine returned " + judgments.size() + " judgments for "
                    + prompts.size() + " prompts");
        }
        List<T> agents = new ArrayList<>(prompts.size());
        for (int i = 0; i < prompts.size(); i++) {
            agents.add(toAgent(prompts.get(i), judgments.get(i), latency));
        }
        return List.copyOf(agents);
    }

    private JudgmentRequest request(String prompt) {
        Objects.requireNonNull(prompt, "prompt");
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        AgentDescriptor descriptor = model.descriptor();
        return new JudgmentRequest(descriptor.name(), prompt, descriptor.model(), descriptor.questionSpecs());
    }

    private <R> R call(Supplier<R> engineCall) {
        try {
            return engineCall.get();
        } catch (JevException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new JevEvaluationException("Jev evaluation of " + model.descriptor().name() + " failed: "
                    + e.getMessage(), e);
        }
    }

    private T toAgent(String prompt, Judgment judgment, Duration latency) {
        Map<Method, Outcome> outcomes = new HashMap<>();
        for (QuestionBinding binding : model.bindings()) {
            QuestionDescriptor question = binding.descriptor();
            Answer answer = answer(judgment, question.key(), question.method());
            NoulAnswer stated = null;
            String statedKey = question.statedKey();
            if (statedKey != null) {
                Answer statedAnswer = answer(judgment, statedKey, question.method());
                if (!(statedAnswer instanceof NoulAnswer noul)) {
                    throw new JevEvaluationException("Expected a NoulAnswer for question '" + statedKey + "'");
                }
                stated = noul;
            }
            outcomes.put(question.method(), binding.mapper().map(answer, stated));
        }
        EvaluationMetadata metadata = new EvaluationMetadata(model.descriptor().name(), prompt, judgment.model(),
                judgment.requestId(), judgment.usage(), latency, judgment.answers());
        ResultInvocationHandler handler = new ResultInvocationHandler(model.descriptor(), outcomes, metadata);
        Object proxy = Proxy.newProxyInstance(model.type().getClassLoader(), proxyInterfaces, handler);
        return model.type().cast(proxy);
    }

    private Answer answer(Judgment judgment, String key, Method method) {
        @Nullable Answer answer = judgment.answers().get(key);
        if (answer == null) {
            throw new JevEvaluationException("Jev returned no answer for question '" + key + "' ("
                    + model.type().getSimpleName() + "." + method.getName() + "())");
        }
        return answer;
    }
}
