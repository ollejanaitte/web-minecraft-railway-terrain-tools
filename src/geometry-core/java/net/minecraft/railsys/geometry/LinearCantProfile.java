package net.minecraft.railsys.geometry;

/**
 * Linear cant ramp from roll0 to roll1 along distance (data model only).
 */
public final class LinearCantProfile implements CantProfile {

	private final double roll0;
	private final double roll1;

	public LinearCantProfile(double roll0, double roll1) {
		RailMath.requireFinite(roll0, "roll0");
		RailMath.requireFinite(roll1, "roll1");
		this.roll0 = roll0;
		this.roll1 = roll1;
	}

	@Override
	public double rollDegAt(double distanceM, double lengthM) {
		if (lengthM <= RailMath.EPS) {
			return roll0;
		}
		double t = RailMath.clamp(distanceM / lengthM, 0.0D, 1.0D);
		return roll0 + (roll1 - roll0) * t;
	}
}
