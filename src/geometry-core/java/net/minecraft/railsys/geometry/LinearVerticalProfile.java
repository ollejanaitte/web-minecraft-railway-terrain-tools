package net.minecraft.railsys.geometry;

/** Linear grade between y0 and y1 over u in [0,1]. */
public final class LinearVerticalProfile implements VerticalProfile {

	private final double y0;
	private final double y1;

	public LinearVerticalProfile(double y0, double y1) {
		RailMath.requireFinite(y0, "y0");
		RailMath.requireFinite(y1, "y1");
		this.y0 = y0;
		this.y1 = y1;
	}

	@Override
	public double yAt(double u) {
		double t = RailMath.clamp01(u);
		return y0 + (y1 - y0) * t;
	}

	@Override
	public double dyDu(double u) {
		return y1 - y0;
	}
}
