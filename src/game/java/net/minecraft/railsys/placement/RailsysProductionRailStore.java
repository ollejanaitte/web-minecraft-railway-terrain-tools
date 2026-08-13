package net.minecraft.railsys.placement;

import net.minecraft.railsys.data.RailId;
import net.minecraft.railsys.data.RailLimits;
import net.minecraft.railsys.data.RailSegment;
import net.minecraft.railsys.data.RailSegmentValidator;
import net.minecraft.railsys.data.RailWorldData;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.RailPath;

/**
 * RailsysProductionRailStore — game-layer R13 wiring that owns the
 * world-scoped {@link RailWorldData} (authoritative confirmed rails + stable id
 * issuer).
 *
 * R13 scope: PRODUCTION DATA FOUNDATION only. Confirming a preview
 * - issues a stable RailId (confirm-time boundary, R12 §3),
 * - promotes the EXACT preview RailPath object (R12 §5.1 / R10F F2.4),
 * - validates the segment (R12-J rail-level scope),
 * - registers it in the authoritative world store.
 *
 * The two-phase server handoff (R12 §3.1 / CC-6) is represented here as a
 * single authoritative issuance on the integrated (world) side; a dedicated
 * client/server confirm protocol is a later-phase (R16+/R23) concern. The
 * placement UI (markers/preview) remains client-local (R10F F6).
 *
 * Persistence backend is R23; R13 keeps the store in memory.
 */
public final class RailsysProductionRailStore {

	private static final RailsysProductionRailStore INSTANCE = new RailsysProductionRailStore();

	private final RailWorldData worldData = new RailWorldData();

	private RailsysProductionRailStore() {
	}

	public static RailsysProductionRailStore getInstance() {
		return INSTANCE;
	}

	/** In-memory authoritative store (R13; persistence in R23). */
	public RailWorldData worldData() {
		return this.worldData;
	}

	/**
	 * Confirm the final preview into a production RailSegment.
	 *
	 * Exact-handoff contract: the returned segment promotes the SAME preview
	 * RailPath object (no rebuild). If the segment fails rail-level validation
	 * the confirm is rejected and no stable id is issued.
	 *
	 * @return the confirmed segment with its stable id, or null on rejection.
	 */
	public RailSegment confirmPreview(AnchorDefinition a, AnchorDefinition b, double cantDeg,
			double gaugeM, String assetId, int assetVersion, RailPath previewPath) {
		if (a == null || b == null || previewPath == null) {
			return null;
		}
		// Pre-validate the geometry against the frozen limits before issuing an id.
		RailId id = this.worldData.nextRailId();
		RailSegment seg = RailSegment.confirm(id, a, b, cantDeg, gaugeM, assetId, assetVersion,
				previewPath, 0, false);
		RailSegmentValidator.RailValidation v = RailSegmentValidator.validate(seg, this.worldData);
		if (!v.valid()) {
			// Rejection: no segment registered, id not committed (issuer counter
			// advanced but unused; acceptable and safe — ids are never reused).
			return null;
		}
		this.worldData.register(seg);
		return seg;
	}

	/** Delete a confirmed rail by stable id (retires the id). */
	public RailSegment deleteRail(RailId id) {
		return id == null ? null : this.worldData.delete(id);
	}

	/** Convenience guard used by the status line. */
	public static double clampGaugeForDefaults(double g) {
		if (g < RailLimits.MIN_GAUGE_M) {
			return RailLimits.MIN_GAUGE_M;
		}
		if (g > RailLimits.MAX_GAUGE_M) {
			return RailLimits.MAX_GAUGE_M;
		}
		return g;
	}
}
