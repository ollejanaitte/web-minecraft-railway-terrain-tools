package net.minecraft.railsys.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;

/**
 * ProductionRailMeshBuilder — builds the PRODUCTION 3D rail mesh from a
 * RailPath (R14-01..07).
 *
 * The RailPath is the single geometry source (F2). Every vertex is computed
 * from {@code PathSample} + {@link RailLocalFrame} (forward/right/up + roll);
 * the rail cross-section (head/web/foot) is laid along the frame's +right and
 * +up axes. Gauge offsets the left/right rails symmetrically around the
 * centerline; changing the profile (gauge) NEVER moves the centerline (F4).
 *
 * Mesh is DERIVED/CACHE (R14-07): it is rebuilt from the authoritative
 * RailSegment + profile, never stored as authoritative data.
 *
 * Sections: the path is split into fixed-length sections (R14-06) so long
 * rails are not one giant buffer. Section boundaries share the exact sample
 * at the split distance (no gap, no frame jump).
 */
public final class ProductionRailMeshBuilder {

	/** Mesh sample step along s (metres). R14 measured default. */
	public static final double DEFAULT_SAMPLE_STEP_M = 0.25D;

	/** Mesh section length (metres) — long rails split here. R14 measured default. */
	public static final double DEFAULT_SECTION_LENGTH_M = 32.0D;

	private ProductionRailMeshBuilder() {
	}

	/** Build the production mesh for a path with a given profile. */
	public static ProductionRailMesh build(RailPath path, RailProfile profile) {
		return build(path, profile, DEFAULT_SAMPLE_STEP_M, DEFAULT_SECTION_LENGTH_M);
	}

	/** Build with explicit sample step and section length. */
	public static ProductionRailMesh build(RailPath path, RailProfile profile,
			double sampleStepM, double sectionLengthM) {
		if (path == null || profile == null) {
			throw new IllegalArgumentException("build requires a path and profile");
		}
		if (!(sampleStepM > 0.0D) || !(sectionLengthM > 0.0D)) {
			throw new IllegalArgumentException("sample step and section length must be positive");
		}
		double total = path.totalLength();
		List<RailMeshSection> sections = new ArrayList<RailMeshSection>();

		double sectionStart = 0.0D;
		int sectionIndex = 0;
		while (sectionStart < total - 1.0E-9D) {
			double sectionEnd = Math.min(sectionStart + sectionLengthM, total);
			sections.add(buildSection(path, profile, sampleStepM, sectionStart, sectionEnd, sectionIndex));
			if (sectionEnd >= total - 1.0E-9D) {
				break;
			}
			sectionStart = sectionEnd;
			sectionIndex++;
		}
		return new ProductionRailMesh(sections, profile, total);
	}

	private static RailMeshSection buildSection(RailPath path, RailProfile profile,
			double step, double sStart, double sEnd, int sectionIndex) {
		List<PathSample> samples = new ArrayList<PathSample>();
		List<double[]> sleepers = new ArrayList<double[]>(); // x,y,z + yaw for sleeper
		int sleeperCount = 0;
		for (double s = sStart; s <= sEnd + 1.0E-9D; s += step) {
			PathSample ps = path.resolve(Math.min(s, sEnd));
			samples.add(ps);
			if (s >= sEnd - 1.0E-9D) {
				break;
			}
		}
		// Sleepers at distance-based s: s = 0, spacing, 2*spacing, ... across the
		// WHOLE path (not sample-index based), clipped to this section. The
		// EXACT path-end sleeper is included; section-internal duplicates are
		// prevented by strict half-open clipping [sStart, sEnd).
		if (profile.hasSleeper) {
			double sp = profile.sleeperSpacingM > 0.0D ? profile.sleeperSpacingM : 0.6D;
			double first = 0.0D;
			double last = path.totalLength();
			// s=0 belongs to section 0 only.
			for (double s = first; s <= last + 1.0E-9D; s += sp) {
				boolean inSection = (s >= sStart - 1.0E-9D) && (s < sEnd - 1.0E-9D)
						|| (s >= last - 1.0E-9D && s <= last + 1.0E-9D && sEnd >= last - 1.0E-9D);
				if (inSection) {
					PathSample ps = path.resolve(Math.min(s, path.totalLength()));
					sleepers.add(new double[] { ps.frame.x, ps.frame.y, ps.frame.z, ps.frame.rollDeg });
					sleeperCount++;
				}
			}
		}
		return new RailMeshSection(sectionIndex, sStart, sEnd, samples, sleepers, sleeperCount);
	}

	/**
	 * Compute the world position of a cross-section corner for a given rail
	 * side. side = -1 left, +1 right. offRight/offUp are the profile corner
	 * offsets from the rail head centre (in the frame's right/up axes).
	 * Pure function; exposed for numeric contract tests.
	 */
	public static double[] cornerWorld(RailLocalFrame f, int side, double gaugeM,
			double offRight, double offUp) {
		double railCentreRight = side * (gaugeM * 0.5D);
		double rx = railCentreRight + offRight;
		double wx = f.x + f.rx * rx + f.ux * offUp;
		double wy = f.y + f.ry * rx + f.uy * offUp;
		double wz = f.z + f.rz * rx + f.uz * offUp;
		return new double[] { wx, wy, wz };
	}

	/** Rail head centre world position for a side. */
	public static double[] railCentre(RailLocalFrame f, int side, double gaugeM) {
		return cornerWorld(f, side, gaugeM, 0.0D, 0.0D);
	}
}
