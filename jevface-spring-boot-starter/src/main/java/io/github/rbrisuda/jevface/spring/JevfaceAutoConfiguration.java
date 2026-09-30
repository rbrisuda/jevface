package io.github.rbrisuda.jevface.spring;

import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.spi.JudgmentEngine;
import io.github.rbrisuda.jevface.typesafe.TypeSafeJudgmentEngine;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Wires typed Jev agents into a Spring Boot application.
 *
 * <ul>
 *   <li>A {@link TypeSafeClient} comes from {@code spring.ai.typesafe.api-key} (the TypeSafe starter) or,
 *       failing that, from the {@code TYPESAFE_API_KEY} / {@code TYPESAFE_BASE_URL} / {@code TYPESAFE_DEFAULT_MODEL}
 *       environment variables.
 *   <li>A {@link JudgmentEngine} bean is created on top of it unless you define your own
 *       (e.g. a {@code StubJudgmentEngine} in tests).
 *   <li>{@link Jevface} is always available. Without an engine, the application still starts and evaluations
 *       fail with a message explaining how to configure a key.
 *   <li>Every {@code @JevAgent} interface in the application's package gets a {@code JevClient<Agent>} bean
 *       (see {@link EnableJevClients} to scan elsewhere).
 * </ul>
 */
@AutoConfiguration(afterName = "org.springaicommunity.typesafe.autoconfigure.TypeSafeAutoConfiguration")
@ConditionalOnClass(Jevface.class)
@Import(AutoConfiguredJevClientsRegistrar.class)
public class JevfaceAutoConfiguration {

    static final String API_KEY = "TYPESAFE_API_KEY";
    static final String BASE_URL = "TYPESAFE_BASE_URL";
    static final String DEFAULT_MODEL = "TYPESAFE_DEFAULT_MODEL";

    @Bean
    @ConditionalOnMissingBean
    public Jevface jevface(ObjectProvider<JudgmentEngine> engine) {
        return Jevface.create(engine.getIfAvailable(UnconfiguredJudgmentEngine::new));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(TypeSafeClient.class)
    static class TypeSafeEngineConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @Conditional(OnTypeSafeApiKeyEnvironment.class)
        TypeSafeClient jevfaceTypeSafeClient(Environment environment) {
            TypeSafeClient.Builder builder = TypeSafeClient.builder().apiKey(environment.getRequiredProperty(API_KEY));
            String baseUrl = environment.getProperty(BASE_URL);
            if (baseUrl != null && !baseUrl.isBlank()) {
                builder.baseUrl(baseUrl);
            }
            String model = environment.getProperty(DEFAULT_MODEL);
            if (model != null && !model.isBlank()) {
                builder.defaultModel(model);
            }
            return builder.build();
        }

        @Bean
        @ConditionalOnMissingBean(JudgmentEngine.class)
        @ConditionalOnBean(TypeSafeClient.class)
        TypeSafeJudgmentEngine typeSafeJudgmentEngine(TypeSafeClient client) {
            return new TypeSafeJudgmentEngine(client);
        }
    }
}
