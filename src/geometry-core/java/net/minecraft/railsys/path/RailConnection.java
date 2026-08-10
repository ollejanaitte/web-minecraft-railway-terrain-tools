package net.minecraft.railsys.path;

import net.minecraft.railsys.geometry.RailMath;

/**
 * RailConnection — declares that two RailEndpoints are physically joined and
 * records the validation verdict. Position and orientation checks follow the
 * frozen Phase 0.6 acceptance:
 *
 * <ul>
 *   <li>join position error &lt;= 1e-4 m</li>
 *   <li>tangent angle error &lt;= 0.5 deg (parallel either direction, so both a
 *       natural continuation and a reverse junction validate)</li>
 *   <li>no self connection, no null endpoint</li>
 * </ul>
 *
 * <p>Duplicate-connection and endpoint-state checks are performed by
 * {@link RailNetwork} (they depend on network state, not the endpoint pair).
 */
public final class RailConnection {

	/** Phase 0.6 frozen join tolerance. */
	public static final double POSITION_TOLERANCE_M = 1.0E-4D;
	/** Phase 0.6 frozen heading continuity tolerance. */
	public static final double ANGLE_TOLERANCE_DEG = 0.5D;

	private final RailEndpoint a;
	private final RailEndpoint b;
	private final RailValidationResult validation;

	private RailConnection(RailEndpoint a, RailEndpoint b, RailValidationResult validation) {
		this.a = a;
		this.b = b;
		this.validation = validation;
	}

	/**
	 * Pure endpoint-pair validation. Returns an invalid result (never throws)
	 * for null endpoints, self connections, position error &gt; tolerance, or
	 * tangent misalignment &gt; tolerance.
	 */
	public static RailValidationResult validate(RailEndpoint a, RailEndpoint b) {
		if (a == null || b == null) {
			return RailValidationResult.invalid("null endpoint");
		}
		if (a == b || a.equals(b)) {
			return RailValidationResult.invalid("self connection");
		}
		double dx = a.x() - b.x();
		double dy = a.y() - b.y();
		double dz = a.z() - b.z();
		double posErr = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (posErr > POSITION_TOLERANCE_M) {
			return RailValidationResult.invalid("position error exceeds tolerance", posErr, 0.0D);
		}
		double angleErr = tangentAngleDeg(a, b);
		if (angleErr > ANGLE_TOLERANCE_DEG) {
			return RailValidationResult.invalid("tangent misalignment exceeds tolerance", posErr, angleErr);
		}
		return RailValidationResult.ok();
	}

	/**
	 * Smallest angle (0..180) between the two native tangents, collapsed to the
	 * 0..90 half-range so parallel (0) and anti-parallel (180) both read as
	 * aligned — a connection may legitimately join pieces facing either way.
	 */
	private static double tangentAngleDeg(RailEndpoint a, RailEndpoint b) {
		double dot = a.tx() * b.tx() + a.ty() * b.ty() + a.tz() * b.tz();
		double magA = RailMath.hypot3(a.tx(), a.ty(), a.tz());
		double magB = RailMath.hypot3(b.tx(), b.ty(), b.tz());
		double cos = magA < RailMath.EPS || magB < RailMath.EPS ? 0.0D : dot / (magA * magB);
		cos = RailMath.clamp(cos, -1.0D, 1.0D);
		double angle = Math.toDegrees(Math.acos(cos));
		return angle > 90.0D ? 180.0D - angle : angle;
	}

	static RailConnection create(RailEndpoint a, RailEndpoint b) {
		RailValidationResult v = validate(a, b);
		RailConnection c = new RailConnection(a, b, v);
		a.addConnection(c);
		b.addConnection(c);
		return c;
	}

	void detach() {
		this.a.removeConnection(this);
		this.b.removeConnection(this);
	}

	public RailEndpoint a() {
		return this.a;
	}

	public RailEndpoint b() {
		return this.b;
	}

	public RailValidationResult validation() {
		return this.validation;
	}

	public boolean isValid() {
		return this.validation.valid;
	}

	public RailEndpoint other(RailEndpoint e) {
		return e == this.a ? this.b : (e == this.b ? this.a : null);
	}
}
