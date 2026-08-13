package net.minecraft.railsys.data;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.ConstantCantProfile;
import net.minecraft.railsys.path.RailPath;

/**
 * RailSegment — the PRODUCTION confirmed rail record (R12 Production Rail Data
 * Model §4.1).
 *
 * AUTHORITATIVE fields (persisted, identity-bearing):
 *   - railId (issued at confirm)
 *   - endpointA / endpointB (RailEndpointData; anchor is the position SSoT)
 *   - assetId / assetVersion (look reference; R10F F4: asset never alters path)
 *   - cantDeg (constant today; CantProfile EXTENSIBLE — CC-1)
 *   - kind (NORMAL/SLOPE/CURVE from F2 fromMarkers decision)
 *   - signalState / occupied (schema-reserved; NOT persisted until Phase 2
 *     writers exist — R12 §2)
 *
 * DERIVED:
 *   - gaugeM snapshot (refreshed from asset at save/render; asset is
 *     authoritative for gauge)
 *   - RailPath (rebuilt from endpoints via F2 fromMarkers; the confirm handoff
 *     promotes the EXACT preview RailPath as the initial derived geometry —
 *     R12 §5.1 / R10F F2.4)
 *   - validationResult (derived, persisted metadata)
 *
 * CACHE:
 *   - frames/mesh are owned by later render phases; not stored here.
 *
 * TRANSIENT:
 *   - the promoted preview RailPath reference is transient in the sense that it
 *     can be re-derived from endpoints; it is retained at confirm for exact
 *     promotion semantics.
 */
public final class RailSegment {

	public enum Kind {
		NORMAL, SLOPE, CURVE
	}

	public enum Lifecycle {
		ACTIVE, RETIRED
	}

	/** F2 pipeline piece id used for derived RailPath rebuilds. */
	public static final int DERIVED_PIECE_ID = 8200;

	private final RailId railId;
	private final Kind kind;
	private final RailEndpointData endpointA;
	private final RailEndpointData endpointB;
	private final String assetId;
	private final int assetVersion;
	private final double cantDeg;
	private final double gaugeM;
	private final int signalState;
	private final boolean occupied;
	private final Map<String, String> metadata = new LinkedHashMap<String, String>();
	private Lifecycle lifecycle;
	private RailPath promotedPreview; // TRANSIENT exact-promotion reference

	private RailSegment(RailId railId, Kind kind, RailEndpointData a, RailEndpointData b,
			String assetId, int assetVersion, double cantDeg, double gaugeM,
			int signalState, boolean occupied) {
		this.railId = railId;
		this.kind = kind;
		this.endpointA = a;
		this.endpointB = b;
		this.assetId = assetId;
		this.assetVersion = assetVersion;
		this.cantDeg = cantDeg;
		this.gaugeM = gaugeM;
		this.signalState = signalState;
		this.occupied = occupied;
		this.lifecycle = Lifecycle.ACTIVE;
	}

	/**
	 * Build a production segment from the FINAL PREVIEW state at confirm.
	 *
	 * R12 §3.1 handoff: this is called AFTER the server accepted the exact
	 * proposed endpoints (acceptFingerprint == previewFingerprint), so the
	 * promoted preview RailPath IS the exact preview object. The caller passes
	 * the exact preview path for promotion; it is re-derivable from endpoints.
	 */
	public static RailSegment confirm(RailId id, AnchorDefinition a, AnchorDefinition b,
			double cantDeg, double gaugeM, String assetId, int assetVersion,
			RailPath previewPath, int signalState, boolean occupied) {
		Kind kind = classify(a, b, cantDeg);
		RailSegment seg = new RailSegment(id, kind,
				new RailEndpointData(a, RailEndpointData.MarkerType.NORMAL,
						RailEndpointData.Placement.CENTER),
				new RailEndpointData(b, RailEndpointData.MarkerType.NORMAL,
						RailEndpointData.Placement.CENTER),
				assetId, assetVersion, cantDeg, gaugeM, signalState, occupied);
		seg.promotedPreview = previewPath;
		return seg;
	}

	private static Kind classify(AnchorDefinition a, AnchorDefinition b, double cantDeg) {
		AnchorDefinition bEnd = b.reversed();
		double da = net.minecraft.railsys.geometry.RailMath.wrapYaw(a.yawDeg - bEnd.yawDeg);
		boolean curve = Math.abs(da) > 1.0D || Math.abs(a.pitchDeg - bEnd.pitchDeg) > 1.0D;
		if (curve) {
			return Kind.CURVE;
		}
		boolean slope = Math.abs(a.y - b.y) > 1.0E-9D;
		return slope ? Kind.SLOPE : Kind.NORMAL;
	}

	/** DERIVED geometry: rebuild from endpoints via the F2 pipeline. */
	public RailPath derivedPath() {
		return RailPath.fromMarkers(endpointA.anchor(), endpointB.anchor(), this.cantDeg, DERIVED_PIECE_ID);
	}

	/** The EXACT preview RailPath promoted at confirm (TRANSIENT; may be null if re-derived). */
	public RailPath promotedPreview() {
		return this.promotedPreview;
	}

	/** DERIVED total length in metres. */
	public double lengthM() {
		RailPath p = this.promotedPreview != null ? this.promotedPreview : derivedPath();
		return p.totalLength();
	}

	public RailId railId() {
		return this.railId;
	}

	public Kind kind() {
		return this.kind;
	}

	public RailEndpointData endpointA() {
		return this.endpointA;
	}

	public RailEndpointData endpointB() {
		return this.endpointB;
	}

	public String assetId() {
		return this.assetId;
	}

	public int assetVersion() {
		return this.assetVersion;
	}

	/** Constant cant degrees (right rail lower positive). */
	public double cantDeg() {
		return this.cantDeg;
	}

	/** DERIVED gauge snapshot (asset is authoritative). */
	public double gaugeM() {
		return this.gaugeM;
	}

	/** Schema-reserved signal state (R-P1; no writer in Phase 1). */
	public int signalState() {
		return this.signalState;
	}

	/** Schema-reserved occupancy (R-P1; no writer in Phase 1). */
	public boolean occupied() {
		return this.occupied;
	}

	public Map<String, String> metadata() {
		return this.metadata;
	}

	public Lifecycle lifecycle() {
		return this.lifecycle;
	}

	/** Retire this segment (delete). Id is NOT reused. */
	public void retire() {
		this.lifecycle = Lifecycle.RETIRED;
	}

	@Override
	public String toString() {
		return "RailSegment{" + railId + " kind=" + kind + " len=" + String.format("%.3f", lengthM())
				+ " asset=" + assetId + "@" + assetVersion + " cant=" + cantDeg + "}";
	}
}
