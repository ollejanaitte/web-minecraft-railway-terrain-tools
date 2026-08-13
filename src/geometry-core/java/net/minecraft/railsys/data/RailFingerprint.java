package net.minecraft.railsys.data;

import java.util.Locale;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * RailFingerprint — deterministic production acceptance identity for the
 * Preview → Confirm exact handoff (R12 §3.1 / §5.1, R10F F2.4).
 *
 * Fingerprint covers the identity-relevant inputs of a rail:
 *   - endpoints (both anchors: x,y,z,yaw,pitch,lengthH,lengthV)
 *   - asset reference (assetId + assetVersion)
 *   - cant
 *   - geometry handle params (from anchors; included via anchors above)
 *   - path identity: length + start/end sample positions + tangents +
 *     sample count at the production spacing (guards against an alternate
 *     geometry pipeline producing a different line).
 *
 * Exact promotion rule (frozen): a confirm is only an EXACT promotion when
 * {@code acceptFingerprint == previewFingerprint}. Any mismatch means the
 * server corrected geometry and the client must re-derive — never promote a
 * different line as if it were the preview.
 *
 * Formatting is Locale.ROOT and uses double.toString() for determinism.
 */
public final class RailFingerprint {

	private final String value;

	private RailFingerprint(String value) {
		this.value = value;
	}

	public static RailFingerprint preview(AnchorDefinition a, AnchorDefinition b,
			double cantDeg, String assetId, int assetVersion, RailPath previewPath) {
		StringBuilder sb = new StringBuilder(256);
		appendAnchor(sb, a);
		sb.append('|');
		appendAnchor(sb, b);
		sb.append('|');
		sb.append(Double.toString(cantDeg));
		sb.append('|');
		sb.append(assetId == null ? "" : assetId);
		sb.append('@');
		sb.append(assetVersion);
		sb.append('|');
		appendPath(sb, previewPath);
		return new RailFingerprint(sb.toString());
	}

	public static RailFingerprint segment(RailSegment seg) {
		return preview(seg.endpointA().anchor(), seg.endpointB().anchor(),
				seg.cantDeg(), seg.assetId(), seg.assetVersion(), seg.promotedPreview());
	}

	private static void appendAnchor(StringBuilder sb, AnchorDefinition a) {
		sb.append(fmt(a.x)).append(',');
		sb.append(fmt(a.y)).append(',');
		sb.append(fmt(a.z)).append(',');
		sb.append(Double.toString(a.yawDeg)).append(',');
		sb.append(Double.toString(a.pitchDeg)).append(',');
		sb.append(Double.toString(a.lengthH_m)).append(',');
		sb.append(Double.toString(a.lengthV_m));
	}

	private static void appendPath(StringBuilder sb, RailPath p) {
		if (p == null) {
			sb.append("null");
			return;
		}
		double total = p.totalLength();
		sb.append(fmt(total));
		PathSample s0 = p.resolve(0.0D);
		PathSample s1 = p.resolve(total);
		sb.append('[').append(fmt(s0.sample.x)).append(',').append(fmt(s0.sample.y)).append(',').append(fmt(s0.sample.z)).append(']');
		sb.append('[').append(fmt(s1.sample.x)).append(',').append(fmt(s1.sample.y)).append(',').append(fmt(s1.sample.z)).append(']');
		sb.append('[').append(fmt(s0.sample.tx)).append(',').append(fmt(s0.sample.ty)).append(',').append(fmt(s0.sample.tz)).append(']');
		sb.append('[').append(fmt(s1.sample.tx)).append(',').append(fmt(s1.sample.ty)).append(',').append(fmt(s1.sample.tz)).append(']');
		int samples = 0;
		for (double s = 0.0D; s <= total + 1.0E-9D; s += 1.0D) {
			p.resolve(Math.min(s, total));
			samples++;
			if (s >= total) {
				break;
			}
		}
		sb.append('n').append(samples);
	}

	private static String fmt(double d) {
		return String.format(Locale.ROOT, "%.6f", d);
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof RailFingerprint && ((RailFingerprint) o).value.equals(this.value));
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return "RailFingerprint{" + this.value + "}";
	}
}
