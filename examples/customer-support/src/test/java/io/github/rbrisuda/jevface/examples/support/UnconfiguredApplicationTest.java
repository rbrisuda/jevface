package io.github.rbrisuda.jevface.examples.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Without a key the app still starts and tells the caller what to configure. */
@SpringBootTest(properties = "TYPESAFE_API_KEY=")
@AutoConfigureMockMvc
class UnconfiguredApplicationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void answersServiceUnavailableWithSetupInstructions() {
        assertThat(mvc.post().uri("/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .bodyJson().extractingPath("$.detail").asString().contains("TYPESAFE_API_KEY");
    }
}
