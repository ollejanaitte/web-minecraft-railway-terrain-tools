package net.minecraft.railsys.render;

import java.util.List;

import net.minecraft.railsys.path.PathSample;

/**
 * RailMeshSection — one mesh section of the production rail (R14-06). A long
 * rail is split into fixed-length sections; the section boundary sample is the
 * exact same PathSample as the next section's first sample (no gap, no frame
 * jump — verified numerically).
 *
 * DERIVED / CACHE data. Not authoritative.
 */
public final class RailMeshSection {

	public final int sectionIndex;
	public final double sStart;
	public final double sEnd;

	/** PathSample at each mesh step within [sStart, sEnd] (inclusive ends). */
	public final List<PathSample> samples;

	/** Sleeper placements at distance-based s (world x,y,z + roll). */
	public final List<double[]> sleepers;
	public final int sleeperCount;

	public RailMeshSection(int sectionIndex, double sStart, double sEnd,
			List<PathSample> samples, List<double[]> sleepers, int sleeperCount) {
		this.sectionIndex = sectionIndex;
		this.sStart = sStart;
		this.sEnd = sEnd;
		this.samples = samples;
		this.sleepers = sleepers;
		this.sleeperCount = sleeperCount;
	}

	/** First sample (used for cross-section boundary continuity). */
	public PathSample firstSample() {
		return this.samples.isEmpty() ? null : this.samples.get(0);
	}

	/** Last sample (used for cross-section boundary continuity). */
	public PathSample lastSample() {
		return this.samples.isEmpty() ? null : this.samples.get(this.samples.size() - 1);
	}
}
