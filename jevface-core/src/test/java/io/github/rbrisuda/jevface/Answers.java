package io.github.rbrisuda.jevface;

import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.ChoiceAnswer;
import io.github.rbrisuda.jevface.spi.Judgment;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.spi.JudgmentRequest;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import io.github.rbrisuda.jevface.spi.ScoreAnswer;
import io.github.rbrisuda.jevface.spi.TokenUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal recording engine for core tests. */
final class Answers implements JudgmentEngine {

    final Map<String, Answer> answers = new LinkedHashMap<>();
    final List<JudgmentRequest> requests = new ArrayList<>();

    static Answers typicalTicket() {
        return new Answers()
                .noul("is_urgent", 0.95)
                .noul("refund_requested", 0.1)
                .noul("churn_risk", 0.4)
                .choice("department", "billing", 0.82, Map.of("billing", 0.88, "technical", 0.12, "sales-team", 0.0))
                .choice("department_detail", "billing", 0.82, Map.of("billing", 0.88, "technical", 0.12))
                .choice("channel", "email", 0.9, Map.of("email", 0.95, "phone", 0.05))
                .choice("product", "payouts", 0.8, Map.of("payouts", 0.9, "cards", 0.1))
                .noul("product_stated", 0.9)
                .score("frustration", 1.6, 0.7, Map.of(0, 0.0, 1, 0.4, 2, 0.6))
                .score("frustration_level", 1.1, 0.9, Map.of(1, 0.95, 2, 0.05))
                .score("politeness", 0.3, 0.8, Map.of(0, 0.7, 1, 0.3))
                .score("politeness_index", 1.7, 0.8, Map.of(2, 0.7, 1, 0.3));
    }

    Answers noul(String key, double value) {
        answers.put(key, new NoulAnswer(value));
        return this;
    }

    Answers choice(String key, String choice, double confidence, Map<String, Double> probabilities) {
        answers.put(key, new ChoiceAnswer(choice, probabilities, confidence));
        return this;
    }

    Answers score(String key, double score, double confidence, Map<Integer, Double> probabilities) {
        answers.put(key, new ScoreAnswer(score, probabilities, confidence));
        return this;
    }

    @Override
    public Judgment judge(JudgmentRequest request) {
        requests.add(request);
        return new Judgment(answers, "jev-1.13.0", "req-1", new TokenUsage(300, 40));
    }
}
