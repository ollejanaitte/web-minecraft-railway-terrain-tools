package net.minecraft.railsys.geometry;

/** Constant elevation. */
public final class FlatVerticalProfile implements VerticalProfile {

	private final double y;

	public FlatVerticalProfile(double y) {
		RailMath.requireFinite(y, "y");
		this.y = y;
	}

	@Override
	public double yAt(double u) {
		return y;
	}

	@Override
	public double dyDu(double u) {
		return 0.0D;
	}
}
