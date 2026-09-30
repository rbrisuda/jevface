package io.github.rbrisuda.jevface.examples.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rbrisuda.jevface.test.StubJudgmentEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The whole application over HTTP; the stub engine bean replaces the real Jev API. */
@SpringBootTest(properties = "TYPESAFE_API_KEY=")
@AutoConfigureMockMvc
class CustomerSupportApplicationTest {

    @TestConfiguration
    static class StubJev {
        @Bean
        StubJudgmentEngine stubJudgmentEngine() {
            StubJudgmentEngine engine = new StubJudgmentEngine();
            engine.forAgent(SupportTriage.class)
                    .noul(SupportTriage::isUrgent, 0.95)
                    .choice(SupportTriage::department, Department.BILLING, 0.81)
                    .scoreLevel(SupportTriage::frustration, Frustration.ANNOYED, 0.92)
                    .choice(SupportTriage::product, Product.PAYOUTS, 0.9)
                    .noul(SupportTriage::refundRequested, 0.05);
            return engine;
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Test
    void routesATicket() {
        assertThat(mvc.post().uri("/tickets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Help! My payouts have been failing for 3 days.\"}"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "queue": "billing",
                          "priority": "P2",
                          "department": "BILLING",
                          "departmentConfidence": 0.81,
                          "frustration": "ANNOYED",
                          "product": "PAYOUTS",
                          "refundRequested": false,
                          "notes": [],
                          "model": "stub"
                        }
                        """);
    }
}
