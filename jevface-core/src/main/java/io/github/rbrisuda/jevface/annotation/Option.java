package io.github.rbrisuda.jevface.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes an option of a {@link ChoiceQuestion} or a level of a {@link ScoreQuestion}.
 *
 * <p>Put it on enum constants, or use it inside {@link ChoiceQuestion#options()}:
 *
 * <pre>{@code
 * enum Department {
 *     @Option(description = "Payments, invoicing, refunds") BILLING,
 *     @Option(description = "Bugs, outages, integrations") TECHNICAL
 * }
 * }</pre>
 *
 * Good descriptions matter: with bare labels Jev's confidence drops noticeably.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {

    /**
     * Label sent to Jev. For enum constants it defaults to the lower-case constant name. Required
     * inside {@link ChoiceQuestion#options()}.
     */
    String label() default "";

    /** What this option means. For Score levels this is the level text sent to Jev. */
    String description() default "";
}
