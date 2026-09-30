package io.github.rbrisuda.jevface.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rbrisuda.jevface.JevClient;
import io.github.rbrisuda.jevface.JevDefinitionException;
import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.spring.app.AppConfig;
import io.github.rbrisuda.jevface.spring.app.Mood;
import io.github.rbrisuda.jevface.spring.other.Spam;
import io.github.rbrisuda.jevface.test.StubJudgmentEngine;
import io.github.rbrisuda.jevface.typesafe.TypeSafeJudgmentEngine;
import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.autoconfigure.TypeSafeAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

class JevfaceAutoConfigurationTest {

    // Blank TYPESAFE_API_KEY overrides a real one in the environment, keeping these tests hermetic.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TypeSafeAutoConfiguration.class, JevfaceAutoConfiguration.class))
            .withUserConfiguration(AppConfig.class)
            .withPropertyValues("TYPESAFE_API_KEY=");

    @Test
    void startsWithoutAnApiKeyAndExplainsHowToConfigureOne() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(Jevface.class).doesNotHaveBean(TypeSafeClient.class)
                    .doesNotHaveBean(JudgmentEngine.class);

            JevClient<Mood> mood = client(context, Mood.class);
            assertThatThrownBy(() -> mood.evaluate("I love it"))
                    .isInstanceOf(JevEvaluationException.class)
                    .hasMessageContaining("Set the TYPESAFE_API_KEY environment variable");
        });
    }

    @Test
    void buildsTheClientFromTheTypeSafeEnvironmentVariables() {
        runner.withPropertyValues("TYPESAFE_API_KEY=ts-test", "TYPESAFE_BASE_URL=http://localhost:8080")
                .run(context -> assertThat(context)
                        .hasSingleBean(TypeSafeClient.class)
                        .hasBean("jevfaceTypeSafeClient")
                        .getBean(JudgmentEngine.class).isInstanceOf(TypeSafeJudgmentEngine.class));
    }

    @Test
    void prefersTheSpringAiTypeSafeStarterWhenItsPropertyIsSet() {
        runner.withPropertyValues("spring.ai.typesafe.api-key=ts-test", "TYPESAFE_API_KEY=ignored")
                .run(context -> assertThat(context)
                        .hasSingleBean(TypeSafeClient.class)
                        .doesNotHaveBean("jevfaceTypeSafeClient")
                        .hasSingleBean(TypeSafeJudgmentEngine.class));
    }

    @Test
    void usesAnApplicationDefinedEngineAndInjectsClientsByGenericType() {
        runner.withPropertyValues("TYPESAFE_API_KEY=ts-test")
                .withBean(StubJudgmentEngine.class, () -> {
                    StubJudgmentEngine engine = new StubJudgmentEngine();
                    engine.forAgent(Mood.class).noul(Mood::happy, 0.9);
                    return engine;
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(JudgmentEngine.class).doesNotHaveBean(TypeSafeJudgmentEngine.class);
                    assertThat(context).hasBean("moodJevClient").doesNotHaveBean("spamJevClient");
                    assertThat(client(context, Mood.class).evaluate("I love it").happy()).isTrue();
                });
    }

    @Test
    void enableJevClientsScansExplicitPackagesInsteadOfTheApplicationPackage() {
        runner.withUserConfiguration(ExplicitScan.class).run(context -> {
            assertThat(context).hasBean("spamJevClient").doesNotHaveBean("moodJevClient").doesNotHaveBean("helperJevClient");
            assertThat(client(context, Spam.class).agentType()).isEqualTo(Spam.class);
        });
    }

    @Test
    void invalidAgentsFailTheStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JevfaceAutoConfiguration.class))
                .withUserConfiguration(BrokenApp.class)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause().isInstanceOf(JevDefinitionException.class)
                        .hasMessageContaining("notAQuestion()"));
    }

    @Test
    void backsOffWhenTheApplicationDefinesJevface() {
        Jevface own = Jevface.create(new StubJudgmentEngine());
        runner.withBean(Jevface.class, () -> own)
                .run(context -> assertThat(context.getBean(Jevface.class)).isSameAs(own));
    }

    @SuppressWarnings("unchecked")
    private static <T> JevClient<T> client(ApplicationContext context, Class<T> agentType) {
        return (JevClient<T>) context.getBeanProvider(ResolvableType.forClassWithGenerics(JevClient.class, agentType))
                .getObject();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJevClients(basePackageClasses = Spam.class)
    static class ExplicitScan {}

    @Configuration(proxyBeanMethods = false)
    @EnableJevClients(basePackages = "io.github.rbrisuda.jevface.spring.broken")
    static class BrokenApp {
        @Bean
        StubJudgmentEngine engine() {
            return new StubJudgmentEngine();
        }
    }
}
