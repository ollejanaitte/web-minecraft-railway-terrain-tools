package net.minecraft.railsys.render;

import java.util.List;

/**
 * ProductionRailMesh — the full production mesh of one rail path, split into
 * sections (R14-06). DERIVED / CACHE — never authoritative.
 */
public final class ProductionRailMesh {

	public final List<RailMeshSection> sections;
	public final RailProfile profile;
	public final double totalLengthM;

	public ProductionRailMesh(List<RailMeshSection> sections, RailProfile profile, double totalLengthM) {
		this.sections = sections;
		this.profile = profile;
		this.totalLengthM = totalLengthM;
	}

	public int sectionCount() {
		return this.sections.size();
	}

	public RailMeshSection section(int i) {
		return this.sections.get(i);
	}

	/** Total sample count across all sections. */
	public int totalSampleCount() {
		int n = 0;
		for (RailMeshSection s : this.sections) {
			n += s.samples.size();
		}
		return n;
	}

	/** Total sleeper count across all sections. */
	public int totalSleeperCount() {
		int n = 0;
		for (RailMeshSection s : this.sections) {
			n += s.sleeperCount;
		}
		return n;
	}
}
