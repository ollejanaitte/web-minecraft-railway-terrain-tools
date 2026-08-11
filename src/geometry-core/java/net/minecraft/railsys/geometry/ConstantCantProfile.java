package net.minecraft.railsys.geometry;

/**
 * Constant cant over the whole piece (Phase 1-R6).
 * rollDeg positive = right rail lower (same contract as CantProfile/RailLocalFrame).
 */
public final class ConstantCantProfile implements CantProfile {

	private final double rollDeg;

	public ConstantCantProfile(double rollDeg) {
		RailMath.requireFinite(rollDeg, "rollDeg");
		this.rollDeg = rollDeg;
	}

	public static ConstantCantProfile of(double rollDeg) {
		return new ConstantCantProfile(rollDeg);
	}

	@Override
	public double rollDegAt(double distanceM, double lengthM) {
		return this.rollDeg;
	}

	public double rollDeg() {
		return this.rollDeg;
	}
}
