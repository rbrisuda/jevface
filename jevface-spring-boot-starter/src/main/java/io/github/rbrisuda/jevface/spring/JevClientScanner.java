package io.github.rbrisuda.jevface.spring;

import io.github.rbrisuda.jevface.JevClient;
import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import java.beans.Introspector;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/** Finds {@code @JevAgent} interfaces and registers a {@code JevClient<Agent>} bean for each. */
final class JevClientScanner extends ClassPathScanningCandidateComponentProvider {

    JevClientScanner() {
        super(false);
        addIncludeFilter(new AnnotationTypeFilter(JevAgent.class, false, true));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    void registerClients(Set<String> basePackages, BeanDefinitionRegistry registry) {
        if (!(registry instanceof BeanFactory beanFactory)) {
            throw new IllegalStateException("JevClient beans need a BeanDefinitionRegistry that is also a BeanFactory");
        }
        ClassLoader classLoader = getResourceLoader().getClassLoader();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : findCandidateComponents(basePackage)) {
                String className = candidate.getBeanClassName();
                if (className != null) {
                    register(ClassUtils.resolveClassName(className, classLoader), registry, beanFactory);
                }
            }
        }
    }

    static String beanName(Class<?> agentType) {
        return Introspector.decapitalize(agentType.getSimpleName()) + "JevClient";
    }

    private static <T> void register(Class<T> agentType, BeanDefinitionRegistry registry, BeanFactory beanFactory) {
        String beanName = beanName(agentType);
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        // Validates the agent interface at startup, so annotation mistakes fail the boot, not the first request.
        RootBeanDefinition definition = new RootBeanDefinition(JevClient.class);
        definition.setTargetType(ResolvableType.forClassWithGenerics(JevClient.class, agentType));
        definition.setInstanceSupplier(() -> beanFactory.getBean(Jevface.class).client(agentType));
        definition.setDescription("JevClient for @JevAgent " + agentType.getName());
        registry.registerBeanDefinition(beanName, definition);
    }
}
