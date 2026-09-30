package io.github.rbrisuda.jevface.spring;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Matches when {@code TYPESAFE_API_KEY} is set (environment variable, system property or Spring property). */
class OnTypeSafeApiKeyEnvironment implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty(JevfaceAutoConfiguration.API_KEY));
    }
}
