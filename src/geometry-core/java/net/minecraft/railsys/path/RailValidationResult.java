package net.minecraft.railsys.path;

/**
 * Lightweight validation result shared by RailPiece / RailConnection / RailPath /
 * RailNetwork. Carries a boolean verdict, a human-readable reason, and optional
 * numeric errors (position metres, angle degrees). Designed to avoid
 * over-engineering while still explaining WHY something is invalid.
 */
public final class RailValidationResult {

	public final boolean valid;
	public final String reason;
	public final double positionErrorM;
	public final double angleErrorDeg;

	private RailValidationResult(boolean valid, String reason, double positionErrorM, double angleErrorDeg) {
		this.valid = valid;
		this.reason = reason;
		this.positionErrorM = positionErrorM;
		this.angleErrorDeg = angleErrorDeg;
	}

	public static RailValidationResult ok() {
		return new RailValidationResult(true, "ok", 0.0D, 0.0D);
	}

	public static RailValidationResult invalid(String reason) {
		return new RailValidationResult(false, reason, 0.0D, 0.0D);
	}

	public static RailValidationResult invalid(String reason, double positionErrorM, double angleErrorDeg) {
		return new RailValidationResult(false, reason, positionErrorM, angleErrorDeg);
	}

	@Override
	public String toString() {
		return valid ? "VALID ok" : "INVALID " + reason
				+ " posErr=" + positionErrorM + " angErr=" + angleErrorDeg;
	}
}
