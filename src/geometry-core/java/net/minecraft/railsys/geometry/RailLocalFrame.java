package net.minecraft.railsys.geometry;

/**
 * Orthonormal rail local frame at distance s.
 * Right-handed {forward, right, up}: right = normalize(forward × worldUp)
 * (flattened, degeneracy -> forward × worldNorth), up = right × forward,
 * then optional roll about forward (cant). Phase 1.1 roll usually 0.
 * With Minecraft Y-up world (+Y up), up points UP (positive Y) for a
 * horizontal track. Positive rollDeg lowers the +right side.
 */
public final class RailLocalFrame {

	public final double x, y, z;
	public final double fx, fy, fz; // forward (unit tangent)
	public final double rx, ry, rz; // right (horizontal-biased)
	public final double ux, uy, uz; // up
	public final double rollDeg;
	public final double distanceM;
	public final int pieceId;

	public RailLocalFrame(double distanceM, double x, double y, double z,
			double fx, double fy, double fz,
			double rx, double ry, double rz,
			double ux, double uy, double uz,
			double rollDeg, int pieceId) {
		this.distanceM = distanceM;
		this.x = x;
		this.y = y;
		this.z = z;
		this.fx = fx;
		this.fy = fy;
		this.fz = fz;
		this.rx = rx;
		this.ry = ry;
		this.rz = rz;
		this.ux = ux;
		this.uy = uy;
		this.uz = uz;
		this.rollDeg = rollDeg;
		this.pieceId = pieceId;
	}

	/**
	 * Build frame from position + tangent + roll.
	 * World up = (0,1,0). If forward nearly vertical, use world +Z as reference.
	 */
	public static RailLocalFrame fromTangent(double distanceM, double x, double y, double z,
			double tx, double ty, double tz, double rollDeg, int pieceId) {
		double[] f = RailMath.copy3(tx, ty, tz);
		RailMath.normalize3(f);

		// right = normalize(forward × worldUp); if degenerate, forward × worldNorth
		double rx = f[1] * 0.0D - f[2] * 1.0D; // f × (0,1,0) = (-fz, 0, fx)
		double ry = f[2] * 0.0D - f[0] * 0.0D;
		double rz = f[0] * 1.0D - f[1] * 0.0D;
		rx = -f[2];
		ry = 0.0D;
		rz = f[0];
		double rn = RailMath.hypot3(rx, ry, rz);
		if (rn < RailMath.EPS) {
			// forward ~ vertical: use world +Z as up-reference → right = f × Z
			rx = f[1] * 1.0D - f[2] * 0.0D;
			ry = f[2] * 0.0D - f[0] * 1.0D;
			rz = f[0] * 0.0D - f[1] * 0.0D;
			rn = RailMath.hypot3(rx, ry, rz);
		}
		rx /= rn;
		ry /= rn;
		rz /= rn;

		// up = right × forward gives the correct upward normal for a
		// right-handed frame {forward, right, up} with Minecraft Y-up world.
		// (Phase 1.2.3 fix: previous code used f × r which yields down.)
		double ux = ry * f[2] - rz * f[1];
		double uy = rz * f[0] - rx * f[2];
		double uz = rx * f[1] - ry * f[0];
		double un = RailMath.hypot3(ux, uy, uz);
		if (un < RailMath.EPS) {
			ux = 0.0D;
			uy = 1.0D;
			uz = 0.0D;
		} else {
			ux /= un;
			uy /= un;
			uz /= un;
		}

		if (Math.abs(rollDeg) > 1.0E-12D) {
			double[] rolled = applyRoll(rx, ry, rz, ux, uy, uz, f[0], f[1], f[2], rollDeg);
			rx = rolled[0];
			ry = rolled[1];
			rz = rolled[2];
			ux = rolled[3];
			uy = rolled[4];
			uz = rolled[5];
		}

		return new RailLocalFrame(distanceM, x, y, z, f[0], f[1], f[2], rx, ry, rz, ux, uy, uz, rollDeg,
				pieceId);
	}

	/** Rotate right/up about forward by rollDeg (positive = right rail lower). */
	private static double[] applyRoll(double rx, double ry, double rz, double ux, double uy, double uz,
			double fx, double fy, double fz, double rollDeg) {
		double a = Math.toRadians(rollDeg);
		double c = Math.cos(a);
		double s = Math.sin(a);
		// With correct up (right × forward), positive roll lowers the +right
		// side: right' = right*cos - up*sin ; up' = right*sin + up*cos.
		double nrx = rx * c - ux * s;
		double nry = ry * c - uy * s;
		double nrz = rz * c - uz * s;
		double nux = rx * s + ux * c;
		double nuy = ry * s + uy * c;
		double nuz = rz * s + uz * c;
		return new double[] { nrx, nry, nrz, nux, nuy, nuz };
	}
}
