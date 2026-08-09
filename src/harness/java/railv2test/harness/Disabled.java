package railv2test.harness;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips a test (reported as SKIPPED, never fails the suite).
 * Use for future/not-yet-implementable contracts (Phase 1+ scaffolds)
 * and for known failures that must NOT be silently enabled.
 * The value is the reason and the owning phase.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Disabled {
    String value();
}
