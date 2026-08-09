package railv2test.harness;

/**
 * Minimal assertion helpers for the dependency-free harness.
 */
public final class Assert {
    private Assert() {
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(double expected, double actual, double tolerance, String message) {
        double diff = Math.abs(expected - actual);
        if (diff > tolerance) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual
                    + " tolerance=" + tolerance + " diff=" + diff);
        }
    }

    public static void assertEqualsInt(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void assertEqualsAngle(double expectedDeg, double actualDeg, double tolerance, String message) {
        double d = expectedDeg - actualDeg;
        while (d > 180.0) {
            d -= 360.0;
        }
        while (d < -180.0) {
            d += 360.0;
        }
        if (Math.abs(d) > tolerance) {
            throw new AssertionError(message + " expected=" + expectedDeg + " actual=" + actualDeg
                    + " tolerance=" + tolerance + " diff=" + Math.abs(d));
        }
    }
}
