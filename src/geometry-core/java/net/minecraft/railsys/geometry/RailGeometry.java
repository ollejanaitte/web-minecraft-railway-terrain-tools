package net.minecraft.railsys.geometry;

/**
 * Phase 1.1 production RailGeometry contract (Phase 0.6 freeze).
 * Distance unit: metre (1 block = 1 m). External API is distance-centred.
 */
public interface RailGeometry {

	/** Total 3D arc length in metres (exact for straight). */
	double lengthM();

	int pieceId();

	/**
	 * Sample at local distance. Out-of-range distances are clamped to
	 * [0, lengthM]. Non-finite distance throws IllegalStateException.
	 */
	RailSample sampleByDistance(double distanceM);

	/** Convenience: progress in [0,1] mapped via arc length. */
	RailSample sampleByProgress(double progress);

	/** Arc length at normalized progress in [0,1]. */
	double lengthAt(double progress);

	/** Cached arc-length table (may be trivial for straight). */
	ArcLengthTable table();

	/** Local frame at distance (position + orthonormal basis + roll). */
	RailLocalFrame frameAt(double distanceM);
}
