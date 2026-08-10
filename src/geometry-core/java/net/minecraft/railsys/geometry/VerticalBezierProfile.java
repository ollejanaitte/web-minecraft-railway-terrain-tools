package net.minecraft.railsys.geometry;

/**
 * Cubic Bezier in Y over u in [0,1] (vertical transition data model).
 * Control heights: y0, y0+dy0, y1-dy1, y1 (handle elevations).
 */
public final class VerticalBezierProfile implements VerticalProfile {

	private final double y0, y1, dy0, dy1;

	public VerticalBezierProfile(double y0, double y1, double dy0, double dy1) {
		RailMath.requireFinite(y0, "y0");
		RailMath.requireFinite(y1, "y1");
		RailMath.requireFinite(dy0, "dy0");
		RailMath.requireFinite(dy1, "dy1");
		this.y0 = y0;
		this.y1 = y1;
		this.dy0 = dy0;
		this.dy1 = dy1;
	}

	@Override
	public double yAt(double u) {
		double t = RailMath.clamp01(u);
		double uu = 1.0D - t;
		double w0 = uu * uu * uu;
		double w1 = 3.0D * uu * uu * t;
		double w2 = 3.0D * uu * t * t;
		double w3 = t * t * t;
		double c1 = y0 + dy0;
		double c2 = y1 - dy1;
		return w0 * y0 + w1 * c1 + w2 * c2 + w3 * y1;
	}

	@Override
	public double dyDu(double u) {
		double t = RailMath.clamp01(u);
		double c1 = y0 + dy0;
		double c2 = y1 - dy1;
		return 3.0D * (1 - t) * (1 - t) * (c1 - y0)
				+ 6.0D * (1 - t) * t * (c2 - c1)
				+ 3.0D * t * t * (y1 - c2);
	}
}
