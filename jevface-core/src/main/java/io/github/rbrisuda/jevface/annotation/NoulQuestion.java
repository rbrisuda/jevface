package io.github.rbrisuda.jevface.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A yes/no question. Jev answers with the probability, between 0 and 1, that the answer is yes.
 *
 * <p>Supported return types:
 * <ul>
 *   <li>{@code boolean} / {@code Boolean}: {@code true} when the answer is at least {@link #threshold()}</li>
 *   <li>{@code double} / {@code Double}: the raw probability</li>
 *   <li>{@link io.github.rbrisuda.jevface.NoulResult}: probability and threshold together</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoulQuestion {

    /** The yes/no question, e.g. {@code "Does this convey urgency?"}. */
    String value();

    /** Optional description of what a yes means. Write it as a statement about the input. */
    String whenTrue() default "";

    /** Optional description of what a no means. Write it as a statement about the input. */
    String whenFalse() default "";

    /** Probability at or above which a {@code boolean} return type is {@code true}. */
    double threshold() default 0.5;

    /** Question id sent to Jev. Defaults to the method name in snake_case. */
    String key() default "";
}
