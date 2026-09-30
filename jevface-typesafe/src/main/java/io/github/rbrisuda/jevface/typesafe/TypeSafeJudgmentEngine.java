package io.github.rbrisuda.jevface.typesafe;

import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.ChoiceAnswer;
import io.github.rbrisuda.jevface.spi.ChoiceSpec;
import io.github.rbrisuda.jevface.spi.Judgment;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.spi.JudgmentRequest;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import io.github.rbrisuda.jevface.spi.NoulSpec;
import io.github.rbrisuda.jevface.spi.QuestionSpec;
import io.github.rbrisuda.jevface.spi.ScoreAnswer;
import io.github.rbrisuda.jevface.spi.ScoreSpec;
import io.github.rbrisuda.jevface.spi.TokenUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.typesafe.JevBatchOptions;
import org.springaicommunity.typesafe.JevBatchResult;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Choice;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springaicommunity.typesafe.response.Usage;

/**
 * Answers typed questions with TypeSafe's Jev API ({@code POST /v1/systemone}).
 *
 * <pre>{@code
 * // Reads TYPESAFE_API_KEY (and optionally TYPESAFE_BASE_URL, e.g. a local Laya server)
 * Jevface jev = Jevface.create(TypeSafeJudgmentEngine.fromEnvironment());
 * }</pre>
 */
public final class TypeSafeJudgmentEngine implements JudgmentEngine {

    private final TypeSafeClient client;
    private final JevBatchOptions batchOptions;

    public TypeSafeJudgmentEngine(TypeSafeClient client) {
        this(client, JevBatchOptions.defaults());
    }

    public TypeSafeJudgmentEngine(TypeSafeClient client, JevBatchOptions batchOptions) {
        this.client = Objects.requireNonNull(client, "client");
        this.batchOptions = Objects.requireNonNull(batchOptions, "batchOptions");
    }

    /**
     * Creates an engine configured from the environment: {@code TYPESAFE_API_KEY} (required),
     * {@code TYPESAFE_BASE_URL} and {@code TYPESAFE_DEFAULT_MODEL} (optional).
     */
    public static TypeSafeJudgmentEngine fromEnvironment() {
        return new TypeSafeJudgmentEngine(TypeSafeClient.builder().build());
    }

    @Override
    public Judgment judge(JudgmentRequest request) {
        return toJudgment(client.systemOne(toSystemOneRequest(request)));
    }

    @Override
    public List<Judgment> judgeAll(List<JudgmentRequest> requests) {
        List<SystemOneRequest> sdkRequests = requests.stream().map(TypeSafeJudgmentEngine::toSystemOneRequest).toList();
        List<Judgment> judgments = new ArrayList<>(requests.size());
        for (JevBatchResult<SystemOneResponse> result : client.systemOneAll(sdkRequests, batchOptions)) {
            SystemOneResponse response = result.value();
            if (!result.succeeded() || response == null) {
                throw new JevEvaluationException("Jev batch request " + result.index() + " failed", result.failure());
            }
            judgments.add(toJudgment(response));
        }
        return judgments;
    }

    static SystemOneRequest toSystemOneRequest(JudgmentRequest request) {
        SystemOneRequest.Builder builder = SystemOneRequest.builder().state(request.state());
        String model = request.model();
        if (model != null) {
            builder.model(model);
        }
        request.questions().forEach((key, spec) -> builder.question(key, toQuestion(spec)));
        return builder.build();
    }

    static Question toQuestion(QuestionSpec spec) {
        return switch (spec) {
            case NoulSpec noul -> {
                Noul.Builder builder = Noul.builder().instructions(noul.instructions());
                String whenTrue = noul.whenTrue();
                String whenFalse = noul.whenFalse();
                if (whenTrue != null) {
                    builder.whenTrue(whenTrue);
                }
                if (whenFalse != null) {
                    builder.whenFalse(whenFalse);
                }
                yield builder.build();
            }
            case ChoiceSpec choice -> {
                Choice.Builder builder = Choice.builder().instructions(choice.instructions());
                for (ChoiceSpec.Option option : choice.options()) {
                    String description = option.description();
                    if (description == null) {
                        builder.option(option.label());
                    } else {
                        builder.option(option.label(), description);
                    }
                }
                yield builder.build();
            }
            case ScoreSpec score -> {
                Score.Builder builder = Score.builder().instructions(score.instructions());
                score.levels().forEach(builder::level);
                yield builder.build();
            }
        };
    }

    static Judgment toJudgment(SystemOneResponse response) {
        Map<String, Answer> answers = new LinkedHashMap<>();
        response.answers().forEach((key, answer) -> answers.put(key, toAnswer(key, answer)));
        return new Judgment(answers, response.model(), response.requestId(), toUsage(response.usage()));
    }

    private static Answer toAnswer(String key, org.springaicommunity.typesafe.response.Answer answer) {
        if (answer instanceof org.springaicommunity.typesafe.response.NoulAnswer noul) {
            return new NoulAnswer(noul.value());
        }
        if (answer instanceof org.springaicommunity.typesafe.response.ChoiceAnswer choice) {
            return new ChoiceAnswer(choice.value(), choice.probabilities(), choice.confidence());
        }
        if (answer instanceof org.springaicommunity.typesafe.response.ScoreAnswer score) {
            return new ScoreAnswer(score.value(), score.probabilities(), score.confidence());
        }
        throw new JevEvaluationException("Unsupported Jev answer type " + answer.type() + " for question '" + key + "'");
    }

    private static TokenUsage toUsage(@Nullable Usage usage) {
        if (usage == null) {
            return TokenUsage.EMPTY;
        }
        Integer input = usage.inputTokens();
        Integer output = usage.outputTokens();
        return new TokenUsage(input != null ? input : 0, output != null ? output : 0);
    }
}
