package net.minecraft.railv2;

/** Small shared math helpers for the Phase 0.1 validation spike. */
public final class RailV2CourseMath {
	private RailV2CourseMath() {
	}

	public static double wrapYaw(double deg) {
		while (deg > 180.0D) {
			deg -= 360.0D;
		}
		while (deg <= -180.0D) {
			deg += 360.0D;
		}
		return deg;
	}
}
