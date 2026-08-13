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
	 * RailPath object (no rebuild). The stable id is issued ONLY after the
	 * rail-level validation passes — a rejected confirm consumes no id (the
	 * issuer counter does not advance past an unused value because we validate
	 * the request BEFORE {@link RailWorldData#nextRailId()}).
	 *
	 * @return the confirmed segment with its stable id, or null on rejection.
	 */
	public RailSegment confirmPreview(AnchorDefinition a, AnchorDefinition b, double cantDeg,
			double gaugeM, String assetId, int assetVersion, RailPath previewPath) {
		if (a == null || b == null || previewPath == null) {
			return null;
		}
		// Validate the request BEFORE issuing an id: build a probe segment with
		// a placeholder id only for validator purposes, then discard it.
		RailSegment probe = RailSegment.confirm(RailId.probe(1L) /* placeholder, discarded */,
				a, b, cantDeg, gaugeM, assetId, assetVersion, previewPath, 0, false);
		RailSegmentValidator.RailValidation pre = RailSegmentValidator.validate(probe, null);
		if (!pre.valid()) {
			return null; // rejected before any id is issued
		}
		// Issue the stable id and register the authoritative segment.
		RailId id = this.worldData.nextRailId();
		RailSegment seg = RailSegment.confirm(id, a, b, cantDeg, gaugeM, assetId, assetVersion,
				previewPath, 0, false);
		this.worldData.register(seg); // register re-validates (R12-J §2.3)
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

	/**
	 * R13 world-binding (Sol review, R23 owner): R13 holds ONE in-memory store
	 * for the running world; per-world persistence/binding lands in R23. This
	 * is invoked by the render-manager world-restore hook when a NEW world is
	 * entered, so rails + retired ids do not leak across world sessions. The id
	 * COUNTER is intentionally NOT reset (monotonic across worlds prevents any
	 * reuse/collision); only active rails + retired set are cleared.
	 */
	public synchronized void resetForNewWorld() {
		this.worldData.clearAll();
	}

	/** Invoked when the integrated world restore sees a new world object. */
	public static void onWorldEnter() {
		getInstance().resetForNewWorld();
	}

	/**
	 * Register the Standard Closed-Loop Production Rail Test Course (R14-12)
	 * into the world store. Uses ONLY production RailSegments. Returns the
	 * registered segments (validated at registration).
	 */
	public java.util.List<RailSegment> registerClosedLoopCourse(double cx, double cz,
			double widthM, double lengthM, double cornerRadiusM, double gaugeM, String assetId) {
		java.util.List<RailSegment> loop = net.minecraft.railsys.course.StandardClosedLoopCourse
				.courseA(cx, cz, widthM, lengthM, cornerRadiusM, gaugeM, assetId);
		java.util.List<RailSegment> registered = new java.util.ArrayList<RailSegment>();
		for (RailSegment s : loop) {
			// Rebuild with a REAL issued id (the course used probe ids).
			RailSegment real = RailSegment.confirm(this.worldData.nextRailId(),
					s.endpointA().anchor(), s.endpointB().anchor(), s.cantDeg(), s.gaugeM(),
					s.assetId(), s.assetVersion(), s.promotedPreview(), 0, false);
			this.worldData.register(real);
			registered.add(real);
		}
		return registered;
	}

	/** Delete all production rails in the store (R15 clear-loop helper). */
	public synchronized int clearAllRails() {
		int n = this.worldData.size();
		this.worldData.clearAll();
		return n;
	}
}
