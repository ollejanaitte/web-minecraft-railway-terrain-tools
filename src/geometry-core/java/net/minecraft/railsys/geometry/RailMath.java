package net.minecraft.railsys.geometry;

/**
 * Shared math / coordinate conventions for Railsys geometry core.
 *
 * <pre>
 * Coordinates: +X east, +Y up, +Z south (Minecraft world).
 * Yaw degrees: 0 = +Z, clockwise-positive from above; wrap (-180, 180].
 *   sample.yaw = atan2(tx, tz)  (NOT negated; entity/renderer apply MC separately)
 * Pitch degrees: positive = nose up (+Y); atan2(ty, hypot(tx,tz)).
 * Roll degrees: positive = right rail lower (lean into curve).
 * EPS = 1e-6 m for direction / length guards.
 * </pre>
 */
public final class RailMath {

	public static final double EPS = 1.0E-6D;

	private RailMath() {
	}

	public static boolean isFinite(double v) {
		return !Double.isNaN(v) && !Double.isInfinite(v);
	}

	public static void requireFinite(double v, String name) {
		if (!isFinite(v)) {
			throw new IllegalArgumentException("Non-finite " + name + "=" + v);
		}
	}

	public static double clamp(double v, double lo, double hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	public static double clamp01(double v) {
		return clamp(v, 0.0D, 1.0D);
	}

	/** Wrap to (-180, 180]. */
	public static double wrapYaw(double deg) {
		while (deg > 180.0D) {
			deg -= 360.0D;
		}
		while (deg <= -180.0D) {
			deg += 360.0D;
		}
		return deg;
	}

	public static double yawFromTangent(double tx, double tz) {
		return wrapYaw(Math.toDegrees(Math.atan2(tx, tz)));
	}

	public static double pitchFromTangent(double tx, double ty, double tz) {
		return Math.toDegrees(Math.atan2(ty, Math.hypot(tx, tz)));
	}

	/**
	 * Unit tangent from yaw/pitch (degrees) using sample conventions.
	 * yaw=0 → +Z; pitch&gt;0 → +Y.
	 */
	public static void unitTangentFromYawPitch(double yawDeg, double pitchDeg, double[] out3) {
		double yaw = Math.toRadians(yawDeg);
		double pitch = Math.toRadians(pitchDeg);
		double cp = Math.cos(pitch);
		out3[0] = Math.sin(yaw) * cp;
		out3[1] = Math.sin(pitch);
		out3[2] = Math.cos(yaw) * cp;
	}

	public static double hypot3(double x, double y, double z) {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public static void normalize3(double[] v) {
		double n = hypot3(v[0], v[1], v[2]);
		if (n < EPS) {
			v[0] = 0.0D;
			v[1] = 0.0D;
			v[2] = 1.0D;
			return;
		}
		v[0] /= n;
		v[1] /= n;
		v[2] /= n;
	}

	public static double[] copy3(double x, double y, double z) {
		return new double[] { x, y, z };
	}
}
