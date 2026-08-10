package railv2test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import railv2test.harness.Disabled;
import railv2test.harness.Test;
import railv2test.tests.ArcLengthTest;
import railv2test.tests.BezierMathTest;
import railv2test.tests.ContinuityScaffoldTest;
import railv2test.tests.FormationScaffoldTest;
import railv2test.tests.KnownFailureDocumentationTest;
import railv2test.tests.PersistenceBaselineTest;
import railv2test.tests.ProductionGeometryTest;
import railv2test.tests.StraightMathTest;
import railv2test.tests.V1ReferenceRegressionTest;

/**
 * Dependency-free test runner for the Railway v2 reference math harness.
 * Runs all @Test static methods of the registered test classes, prints a
 * summary, and exits non-zero if any enabled test fails.
 *
 * Usage: ./gradlew harnessTest   (wires into `check`)
 */
public final class Runner {

    private static final Class<?>[] TEST_CLASSES = {
            StraightMathTest.class,
            BezierMathTest.class,
            ArcLengthTest.class,
            ContinuityScaffoldTest.class,
            FormationScaffoldTest.class,
            V1ReferenceRegressionTest.class,
            PersistenceBaselineTest.class,
            KnownFailureDocumentationTest.class,
            ProductionGeometryTest.class,
    };

    private static final class Case {
        final String name;
        final Runnable body;
        final String disabledReason;

        Case(String name, Runnable body, String disabledReason) {
            this.name = name;
            this.body = body;
            this.disabledReason = disabledReason;
        }
    }

    public static void main(String[] args) {
        List<Case> cases = new ArrayList<>();
        for (Class<?> clazz : TEST_CLASSES) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(Test.class) || !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Disabled dis = m.getAnnotation(Disabled.class);
                String reason = dis != null ? dis.value() : null;
                cases.add(new Case(clazz.getSimpleName() + "." + m.getName(),
                        () -> invoke(m), reason));
            }
        }

        int passed = 0;
        int failed = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        System.out.println("=== Railway v2 reference math harness ===");
        System.out.println("Tests discovered: " + cases.size());
        for (Case c : cases) {
            if (c.disabledReason != null) {
                skipped++;
                System.out.println("[SKIP ] " + c.name + "  (" + c.disabledReason + ")");
                continue;
            }
            try {
                c.body.run();
                passed++;
                System.out.println("[PASS ] " + c.name);
            } catch (Throwable t) {
                failed++;
                failures.add(c.name + " -> " + t);
                System.out.println("[FAIL ] " + c.name + " : " + t);
            }
        }
        System.out.println("==========================================");
        System.out.println("PASSED=" + passed + " FAILED=" + failed + " SKIPPED=" + skipped);
        if (!failures.isEmpty()) {
            System.out.println("--- failures ---");
            for (String f : failures) {
                System.out.println("  " + f);
            }
        }
        System.out.println("RESULT: " + (failed == 0 ? "SUCCESS" : "FAILURE"));
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void invoke(Method m) {
        try {
            m.invoke(null);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }
}
