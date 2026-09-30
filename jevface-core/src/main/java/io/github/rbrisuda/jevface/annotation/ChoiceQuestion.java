package io.github.rbrisuda.jevface.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pick exactly one option from a fixed set. Jev answers with the chosen option, a probability per
 * option and a confidence.
 *
 * <p>The options come from the return type:
 * <ul>
 *   <li>an {@code enum}: every constant is an option; describe constants with {@link Option}</li>
 *   <li>{@code String}: options are listed in {@link #options()}</li>
 * </ul>
 *
 * <p>Supported return types: {@code E}, {@code String}, {@link io.github.rbrisuda.jevface.ChoiceResult
 * ChoiceResult&lt;E&gt;}, {@code ChoiceResult<String>}, and any of these wrapped in
 * {@link java.util.Optional}.
 *
 * <p>A Choice always names a winner, because its probabilities sum to one. When the input might not
 * be about any of the options, set {@link #stated()}: an additional yes/no question that decides
 * whether the answer is present at all. The return type must then be an {@code Optional}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ChoiceQuestion {

    /** What Jev should decide, e.g. {@code "Which team should handle this?"}. */
    String value();

    /** Options for {@code String} return types. Must be empty for enum return types. */
    Option[] options() default {};

    /**
     * Optional yes/no gate, e.g. {@code "Does the customer name a specific product?"}. When Jev's
     * answer is below {@link #statedThreshold()}, the method returns {@code Optional.empty()}.
     */
    String stated() default "";

    /** Threshold for {@link #stated()}. */
    double statedThreshold() default 0.5;

    /**
     * Confidence floor between 0 and 1. A negative value inherits {@link JevAgent#minConfidence()}.
     * Below the floor an {@code Optional} return type is empty, a plain value throws
     * {@link io.github.rbrisuda.jevface.LowConfidenceException}, and a {@code ChoiceResult} is returned as is.
     */
    double minConfidence() default -1;

    /** Question id sent to Jev. Defaults to the method name in snake_case. */
    String key() default "";
}
