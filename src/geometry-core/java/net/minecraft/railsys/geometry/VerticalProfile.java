package net.minecraft.railsys.geometry;

/**
 * Vertical elevation profile over horizontal progress u in [0,1] or horizontal distance.
 * Phase 1.1: flat / linear / cubic Bezier-in-Y. Does not claim RTM vertical-curve parity.
 */
public interface VerticalProfile {

	/** Elevation Y at normalized horizontal parameter u in [0,1]. */
	double yAt(double u);

	/** dy/du at u (for pitch when composed with horizontal speed). */
	double dyDu(double u);
}
