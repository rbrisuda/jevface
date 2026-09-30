package io.github.rbrisuda.jevface.spring;

import java.util.LinkedHashSet;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers {@code JevClient} beans for {@code @JevAgent} interfaces in the {@code @SpringBootApplication}
 * package, unless the application uses {@link EnableJevClients} explicitly.
 */
class AutoConfiguredJevClientsRegistrar
        implements ImportBeanDefinitionRegistrar, BeanFactoryAware, EnvironmentAware, ResourceLoaderAware {

    private final JevClientScanner scanner = new JevClientScanner();
    private @Nullable BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        scanner.setEnvironment(environment);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        scanner.setResourceLoader(resourceLoader);
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        BeanFactory factory = beanFactory;
        if (factory == null || registry.containsBeanDefinition(JevClientsRegistrar.MARKER_BEAN_NAME)
                || !AutoConfigurationPackages.has(factory)) {
            return;
        }
        scanner.registerClients(new LinkedHashSet<>(AutoConfigurationPackages.get(factory)), registry);
    }
}
