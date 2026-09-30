package io.github.rbrisuda.jevface.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AgentIntrospectorTest {

    @ParameterizedTest
    @CsvSource({
        "isUrgent, is_urgent",
        "getDepartment, department",
        "department, department",
        "refundRequested, refund_requested",
        "URLCheck, url_check",
        "score2Level, score2_level",
        "getter, getter",
    })
    void derivesSnakeCaseQuestionIds(String methodName, String key) {
        assertThat(AgentIntrospector.defaultKey(methodName)).isEqualTo(key);
    }
}
