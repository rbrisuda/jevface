package io.github.rbrisuda.jevface.spring;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/** Backs {@link EnableJevClients}. */
class JevClientsRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    /** Presence of this bean definition switches off scanning of the auto-configuration packages. */
    static final String MARKER_BEAN_NAME = JevClientsRegistrar.class.getName() + ".explicit";

    private final JevClientScanner scanner = new JevClientScanner();

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
        if (!registry.containsBeanDefinition(MARKER_BEAN_NAME)) {
            registry.registerBeanDefinition(MARKER_BEAN_NAME, new RootBeanDefinition(Object.class));
        }
        scanner.registerClients(basePackages(metadata), registry);
    }

    private static Set<String> basePackages(AnnotationMetadata metadata) {
        MergedAnnotation<EnableJevClients> annotation = metadata.getAnnotations().get(EnableJevClients.class);
        Set<String> packages = new LinkedHashSet<>();
        if (annotation.isPresent()) {
            packages.addAll(Arrays.asList(annotation.getStringArray("basePackages")));
            for (Class<?> type : annotation.getClassArray("basePackageClasses")) {
                packages.add(type.getPackageName());
            }
        }
        if (packages.isEmpty()) {
            packages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return packages;
    }
}
