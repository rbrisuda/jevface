package io.github.rbrisuda.jevface.typesafe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.rbrisuda.jevface.EvaluationMetadata;
import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.Jevface;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.RetryPolicy;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Exercises the real TypeSafe SDK over HTTP against a mocked Jev endpoint. No API key needed. */
class TypeSafeJudgmentEngineTest {

    private static final String PROMPT = "Help! My payouts have been failing for 3 days.";

    private static final String EXPECTED_REQUEST = """
            {
              "state": "Help! My payouts have been failing for 3 days.",
              "model": "jev-latest",
              "questions": {
                "department": {
                  "type": "choice",
                  "instructions": "Which team should handle this?",
                  "criteria": {
                    "billing": "Payments, invoicing, refunds",
                    "technical": "Bugs, outages, integrations",
                    "sales": "Pricing, upgrades, new accounts"
                  }
                },
                "frustration": {
                  "type": "score",
                  "instructions": "How frustrated is the customer?",
                  "criteria": ["Calm", "Frustrated", "Very angry"]
                },
                "is_urgent": {
                  "type": "noul",
                  "instructions": "Does this convey urgency?",
                  "criteria": { "true": "Explicitly time-sensitive", "false": "No urgency expressed" }
                },
                "product": {
                  "type": "choice",
                  "instructions": "Which product is affected?",
                  "criteria": { "payouts": null, "cards": "Debit and credit cards" }
                },
                "product_stated": {
                  "type": "noul",
                  "instructions": "Does the customer name the affected product?"
                }
              }
            }
            """;

    private static final String RESPONSE = """
            {
              "model": "jev-1.13.0",
              "answers": {
                "is_urgent": { "type": "noul", "noul": 0.95 },
                "department": {
                  "type": "choice",
                  "choice": "billing",
                  "probabilities": { "billing": 0.88, "technical": 0.12, "sales": 0.0 },
                  "confidence": 0.81
                },
                "frustration": {
                  "type": "score",
                  "score": 1.05,
                  "legend": { "0": "Calm", "1": "Frustrated", "2": "Very angry" },
                  "probabilities": { "0": 0.0, "1": 0.95, "2": 0.05 },
                  "confidence": 0.92
                },
                "product": {
                  "type": "choice",
                  "choice": "payouts",
                  "probabilities": { "payouts": 0.97, "cards": 0.03 },
                  "confidence": 0.94
                },
                "product_stated": { "type": "noul", "noul": 0.91 }
              },
              "usage": { "input_tokens": 318, "output_tokens": 73 }
            }
            """;

    private MockRestServiceServer server;
    private Jevface jev;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClient = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClient).ignoreExpectOrder(true).build();
        TypeSafeClient client = TypeSafeClient.builder()
                .apiKey("test-key")
                .baseUrl("https://jev.test")
                .restClientBuilder(restClient)
                .retryPolicy(RetryPolicy.noRetry())
                .build();
        jev = Jevface.create(new TypeSafeJudgmentEngine(client));
    }

    @Test
    void sendsTheAgentAsOneSystemOneRequestAndMapsTheAnswers() {
        server.expect(requestTo("https://jev.test/v1/systemone"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().json(EXPECTED_REQUEST, JsonCompareMode.STRICT))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        PayoutTriage triage = jev.evaluate(PayoutTriage.class, PROMPT);

        server.verify();
        assertThat(triage.isUrgent()).isTrue();
        assertThat(triage.department()).isEqualTo(PayoutTriage.Department.BILLING);
        assertThat(triage.frustration().level()).isEqualTo("Frustrated");
        assertThat(triage.frustration().value()).isEqualTo(1.05);
        assertThat(triage.frustration().probabilityOf("Very angry")).isEqualTo(0.05);
        assertThat(triage.product()).contains("payouts");

        EvaluationMetadata metadata = Jevface.evaluationOf(triage);
        assertThat(metadata.model()).isEqualTo("jev-1.13.0");
        assertThat(metadata.usage().inputTokens()).isEqualTo(318);
        assertThat(metadata.usage().outputTokens()).isEqualTo(73);
    }

    @Test
    void evaluatesBatches() {
        server.expect(twice(), requestTo("https://jev.test/v1/systemone"))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        List<PayoutTriage> results = jev.client(PayoutTriage.class).evaluateAll(List.of(PROMPT, "Another ticket"));

        server.verify();
        assertThat(results).extracting(PayoutTriage::department).containsOnly(PayoutTriage.Department.BILLING);
    }

    @Test
    void surfacesApiErrorsAsEvaluationExceptions() {
        server.expect(requestTo("https://jev.test/v1/systemone"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"type\":\"authentication_error\",\"message\":\"Invalid API key\"}}"));

        assertThatThrownBy(() -> jev.evaluate(PayoutTriage.class, PROMPT))
                .isInstanceOf(JevEvaluationException.class)
                .hasMessageContaining("PayoutTriage");
    }
}
