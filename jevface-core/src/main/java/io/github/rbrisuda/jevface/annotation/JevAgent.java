package io.github.rbrisuda.jevface.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a Jev agent: a typed questionnaire that is answered in a single Jev call.
 *
 * <p>Every abstract method of the interface is one question and must carry exactly one of
 * {@link NoulQuestion}, {@link ChoiceQuestion} or {@link ScoreQuestion}. The method's return type
 * decides how the answer is exposed to your code. {@code default} methods are never sent to Jev;
 * use them to compose answers in code.
 *
 * <pre>{@code
 * @JevAgent(description = "Triage of incoming support tickets")
 * public interface SupportTriage {
 *
 *     @NoulQuestion("Does this convey urgency?")
 *     boolean isUrgent();
 *
 *     @ChoiceQuestion("Which team should handle this?")
 *     Department department();
 * }
 *
 * SupportTriage triage = jevface.evaluate(SupportTriage.class, "My payouts have failed for 3 days!");
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JevAgent {

    /** Agent name used in logs and diagnostics. Defaults to the interface's simple name. */
    String name() default "";

    /** Human-readable description. Documentation only; it is not sent to Jev. */
    String description() default "";

    /** Jev model to use, e.g. {@code jev-latest}. Empty means the engine's default model. */
    String model() default "";

    /**
     * Default confidence floor for every Choice and Score question of this agent, between 0 and 1.
     * Individual questions can override it. 0 disables the floor.
     */
    double minConfidence() default 0.0;
}
