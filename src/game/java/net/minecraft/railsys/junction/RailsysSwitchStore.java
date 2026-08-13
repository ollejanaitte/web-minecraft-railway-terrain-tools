package net.minecraft.railsys.junction;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.railsys.data.RailSegment;

/**
 * RailsysSwitchStore — game-layer R17 wiring owning a world-scoped
 * {@link SwitchNetwork} over confirmed {@link RailSegment}s.
 *
 * A junction is registered over the production rail store's segments. It never
 * creates/deletes rails; it only records explicit switch routes on top of the
 * confirmed network (in-session; reload restore is R23).
 */
public final class RailsysSwitchStore {

	private static final RailsysSwitchStore INSTANCE = new RailsysSwitchStore();

	private final SwitchNetwork network = new SwitchNetwork();

	private RailsysSwitchStore() {
	}

	public static RailsysSwitchStore getInstance() {
		return INSTANCE;
	}

	public SwitchNetwork network() {
		return this.network;
	}

	public synchronized void resetForNewWorld() {
		this.network.clear();
	}

	/**
	 * Register a switch spur junction on the FIRST loop straight: the corner
	 * before it is mainIn, the straight is mainOut, and a new branch diverges
	 * by switchDeg. The branch is registered into the world store. Returns the
	 * junction (null on validation failure).
	 */
	public synchronized SwitchJunction registerLoopSpur(double switchDeg, String assetId) {
		net.minecraft.railsys.placement.RailsysProductionRailStore store =
				net.minecraft.railsys.placement.RailsysProductionRailStore.getInstance();
		List<RailSegment> rails = new ArrayList<RailSegment>(store.worldData().rails());
		if (rails.size() < 8) {
			return null;
		}
		// Rebuild an ordered loop view: rails are rail-1..rail-8 in order.
		RailSegment mainIn = null;
		RailSegment mainOut = null;
		for (RailSegment r : rails) {
			if (r.railId().value() == 8L) {
				mainIn = r; // corner 8 ends at straight 1 start
			}
			if (r.railId().value() == 1L) {
				mainOut = r; // straight 1
			}
		}
		if (mainIn == null || mainOut == null) {
			return null;
		}
		net.minecraft.railsys.geometry.AnchorDefinition start = mainOut.endpointA().anchor();
		double branchYaw = net.minecraft.railsys.geometry.RailMath.wrapYaw(start.yawDeg + switchDeg);
		// Branch: from the node heading branchYaw, length ~20m, slightly
		// diverging laterally so it is visible.
		double bx = start.x + Math.sin(Math.toRadians(branchYaw)) * 20.0D;
		double bz = start.z + Math.cos(Math.toRadians(branchYaw)) * 20.0D;
		net.minecraft.railsys.geometry.AnchorDefinition branchStart =
				new net.minecraft.railsys.geometry.AnchorDefinition(start.x, start.y, start.z,
						branchYaw, 0.0D, 1.0D, 0.0D);
		net.minecraft.railsys.geometry.AnchorDefinition branchEnd =
				new net.minecraft.railsys.geometry.AnchorDefinition(bx, start.y, bz,
						net.minecraft.railsys.geometry.RailMath.wrapYaw(branchYaw + 180.0D), 0.0D, 1.0D, 0.0D);
		// Build the branch's preview path explicitly (exact promotion).
		net.minecraft.railsys.path.RailPath branchPath =
				net.minecraft.railsys.path.RailPath.fromMarkers(branchStart, branchEnd, 0.0D, 8201);
		RailSegment branch = store.confirmPreview(branchStart, branchEnd,
				0.0D, mainOut.gaugeM(), assetId, 1, branchPath);
		if (branch == null) {
			return null;
		}
		List<RailSegment> branches = new ArrayList<RailSegment>();
		branches.add(branch);
		return this.network.registerJunction(1L, mainIn, mainOut, branches);
	}
}
