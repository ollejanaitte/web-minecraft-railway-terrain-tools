package net.minecraft.railsys.data;

import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.RailPath;

/**
 * RailSegmentValidator — rail-LEVEL validity checks (R12-J §2; R13 scope is a
 * single RailSegment, not network topology).
 *
 * Checks (R13): nulls, finite anchors, min/max length, gradient limit, cant
 * limit, gauge range, asset reference present, duplicate/retired id (via the
 * world store), lifecycle state, zero length.
 *
 * NOT in R13 (deferred): connection topology (R16), snap (R16), junction (R17),
 * switch route/animation (R17/18), connector target (R19), persistence
 * cross-reference (R23).
 *
 * This is a PURE function over the data model — reusable at preview, confirm,
 * and future load without UI coupling.
 */
public final class RailSegmentValidator {

	public enum Severity {
		VALID, INVALID, DEGRADED
	}

	private RailSegmentValidator() {
	}

	/** Validate a segment WITHOUT a world store (no id-lifetime checks). */
	public static RailValidation validate(RailSegment seg) {
		return validate(seg, null);
	}

	/**
	 * Validate a segment, optionally against a world store for duplicate/
	 * retired id checks.
	 */
	public static RailValidation validate(RailSegment seg, RailWorldData world) {
		if (seg == null) {
			return RailValidation.invalid("null segment");
		}
		if (seg.railId() == null) {
			return RailValidation.invalid("segment has no stable id");
		}
		if (seg.lifecycle() == RailSegment.Lifecycle.RETIRED) {
			return RailValidation.invalid("segment is retired");
		}
		if (world != null) {
			if (world.issuer().isRetired(seg.railId())) {
				return RailValidation.invalid("retired id: " + seg.railId());
			}
			RailSegment existing = world.get(seg.railId());
			if (existing != null && existing != seg) {
				return RailValidation.invalid("duplicate id: " + seg.railId());
			}
		}

		RailEndpointData a = seg.endpointA();
		RailEndpointData b = seg.endpointB();
		if (a == null || b == null) {
			return RailValidation.invalid("missing endpoint");
		}
		if (!finite(a) || !finite(b)) {
			return RailValidation.invalid("non-finite endpoint anchor");
		}

		// zero length / min / max
		double len;
		try {
			len = seg.lengthM();
		} catch (RuntimeException e) {
			return RailValidation.invalid("geometry build failed: " + e.getMessage());
		}
		if (!(len > 0.0D) || !RailMath.isFinite(len)) {
			return RailValidation.invalid("zero or non-finite length");
		}
		if (len < RailLimits.MIN_RAIL_LENGTH_M) {
			return RailValidation.invalid("too short: " + len + " < " + RailLimits.MIN_RAIL_LENGTH_M);
		}
		if (len > RailLimits.MAX_RAIL_LENGTH_M) {
			return RailValidation.invalid("too long: " + len + " > " + RailLimits.MAX_RAIL_LENGTH_M);
		}

		// gradient / pitch limit — check endpoint pitches AND the internal
		// path maximum gradient (rail-level: a single segment must not exceed
		// the gradient limit anywhere along it).
		if (Math.abs(a.anchor().pitchDeg) > RailLimits.MAX_GRADIENT_DEG
				|| Math.abs(b.anchor().pitchDeg) > RailLimits.MAX_GRADIENT_DEG) {
			return RailValidation.invalid("gradient exceeds " + RailLimits.MAX_GRADIENT_DEG + " deg");
		}
		RailPath derived = seg.derivedPath();
		double step = Math.max(0.5D, len / 64.0D);
		double maxPitch = 0.0D;
		for (double s = 0.0D; s <= len + 1.0E-9D; s += step) {
			double p = Math.abs(derived.resolve(Math.min(s, len)).sample.pitchDeg);
			if (p > maxPitch) {
				maxPitch = p;
			}
			if (s >= len) {
				break;
			}
		}
		if (maxPitch > RailLimits.MAX_GRADIENT_DEG) {
			return RailValidation.invalid("path gradient exceeds " + RailLimits.MAX_GRADIENT_DEG
					+ " deg (max " + maxPitch + ")");
		}

		// cant limit — must also reject NaN/Inf (Math.abs(NaN)>x is false).
		double cant = seg.cantDeg();
		if (!RailMath.isFinite(cant) || Math.abs(cant) > RailLimits.MAX_CANT_DEG) {
			return RailValidation.invalid("cant exceeds " + RailLimits.MAX_CANT_DEG + " deg or is non-finite");
		}

		// gauge range
		double g = seg.gaugeM();
		if (!(g >= RailLimits.MIN_GAUGE_M && g <= RailLimits.MAX_GAUGE_M)) {
			return RailValidation.invalid("gauge out of range: " + g);
		}

		// asset reference
		if (seg.assetId() == null || seg.assetId().isEmpty()) {
			return RailValidation.invalid("missing asset id");
		}

		// promoted-preview vs authoritative-endpoint consistency: if a promoted
		// preview exists it MUST describe the same line as the endpoints (a
		// phantom/mismatched path is rejected, not silently accepted). Compare
		// length AND start/end world positions + tangents (a same-length but
		// shifted/differently-shaped path is still rejected).
		RailPath promoted = seg.promotedPreview();
		if (promoted != null) {
			double derivedLen = derived.totalLength();
			double promotedLen = promoted.totalLength();
			if (!RailMath.isFinite(promotedLen)
					|| Math.abs(promotedLen - derivedLen) > 1.0E-6D) {
				return RailValidation.invalid("promoted preview length " + promotedLen
						+ " != derived length " + derivedLen);
			}
			net.minecraft.railsys.path.PathSample d0 = derived.resolve(0.0D);
			net.minecraft.railsys.path.PathSample d1 = derived.resolve(derivedLen);
			net.minecraft.railsys.path.PathSample p0 = promoted.resolve(0.0D);
			net.minecraft.railsys.path.PathSample p1 = promoted.resolve(promotedLen);
			if (!samePos(d0, p0) || !samePos(d1, p1)) {
				return RailValidation.invalid("promoted preview start/end positions differ from derived");
			}
			if (!sameTangent(d0, p0) || !sameTangent(d1, p1)) {
				return RailValidation.invalid("promoted preview start/end tangents differ from derived");
			}
		}

		return RailValidation.ok();
	}

	private static boolean samePos(net.minecraft.railsys.path.PathSample a, net.minecraft.railsys.path.PathSample b) {
		return Math.abs(a.sample.x - b.sample.x) <= 1.0E-4D
				&& Math.abs(a.sample.y - b.sample.y) <= 1.0E-4D
				&& Math.abs(a.sample.z - b.sample.z) <= 1.0E-4D;
	}

	private static boolean sameTangent(net.minecraft.railsys.path.PathSample a, net.minecraft.railsys.path.PathSample b) {
		return Math.abs(a.sample.tx - b.sample.tx) <= 1.0E-4D
				&& Math.abs(a.sample.ty - b.sample.ty) <= 1.0E-4D
				&& Math.abs(a.sample.tz - b.sample.tz) <= 1.0E-4D;
	}

	private static boolean finite(RailEndpointData e) {
		net.minecraft.railsys.geometry.AnchorDefinition an = e.anchor();
		return RailMath.isFinite(an.x) && RailMath.isFinite(an.y) && RailMath.isFinite(an.z)
				&& RailMath.isFinite(an.yawDeg) && RailMath.isFinite(an.pitchDeg)
				&& RailMath.isFinite(an.lengthH_m) && RailMath.isFinite(an.lengthV_m);
	}

	/** Result of a rail-level validation. */
	public static final class RailValidation {
		public final Severity severity;
		public final String reason;

		private RailValidation(Severity severity, String reason) {
			this.severity = severity;
			this.reason = reason;
		}

		public static RailValidation ok() {
			return new RailValidation(Severity.VALID, "ok");
		}

		public static RailValidation invalid(String reason) {
			return new RailValidation(Severity.INVALID, reason);
		}

		public boolean valid() {
			return this.severity == Severity.VALID;
		}

		@Override
		public String toString() {
			return "RailValidation{" + severity + " " + reason + "}";
		}
	}
}
