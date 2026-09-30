package io.github.rbrisuda.jevface.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Registers a {@code JevClient<Agent>} bean for every {@code @JevAgent} interface in the given packages
 * (default: the package of the annotated class). Only needed when agents live outside the Spring Boot
 * application's package; otherwise they are found automatically.
 *
 * <pre>{@code
 * @Service
 * class TicketRouter {
 *     TicketRouter(JevClient<SupportTriage> triage) { ... }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(JevClientsRegistrar.class)
public @interface EnableJevClients {

    /** Packages to scan for {@code @JevAgent} interfaces. */
    String[] basePackages() default {};

    /** Type-safe alternative to {@link #basePackages()}: the packages of these classes are scanned. */
    Class<?>[] basePackageClasses() default {};
}
