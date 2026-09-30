package io.github.rbrisuda.jevface.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place the input on an ordered rubric of 2 to 10 levels. Jev answers with a continuous value
 * (level 0 is the first level; 1.1 sits just past the second level), a probability per level and a
 * confidence.
 *
 * <p>The levels come from the return type:
 * <ul>
 *   <li>an {@code enum}: constants in declaration order are the levels; describe them with {@link Option}</li>
 *   <li>otherwise: levels are listed in {@link #levels()}</li>
 * </ul>
 *
 * <p>Supported return types: {@code double} (raw value), {@code int} (nearest level index),
 * {@code E} or {@code String} (nearest level), {@link io.github.rbrisuda.jevface.ScoreResult ScoreResult&lt;E&gt;},
 * {@code ScoreResult<String>}, and any of these wrapped in {@link java.util.Optional}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ScoreQuestion {

    /** What Jev should rate, e.g. {@code "How frustrated is the customer?"}. */
    String value();

    /** Ordered level descriptions, lowest first. Must be empty for enum-based return types. */
    String[] levels() default {};

    /** Optional yes/no gate; see {@link ChoiceQuestion#stated()}. Requires an {@code Optional} return type. */
    String stated() default "";

    /** Threshold for {@link #stated()}. */
    double statedThreshold() default 0.5;

    /** Confidence floor; see {@link ChoiceQuestion#minConfidence()}. */
    double minConfidence() default -1;

    /** Question id sent to Jev. Defaults to the method name in snake_case. */
    String key() default "";
}
