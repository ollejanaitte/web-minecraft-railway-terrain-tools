package net.minecraft.railsys.junction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.railsys.data.RailSegment;

/**
 * SwitchNetwork — the production switch-junction registry (R17).
 *
 * Owns {@link SwitchJunction}s over confirmed {@link RailSegment}s. A junction
 * is registered at a node with a main-in, a main-out and at least one branch.
 * Each registration validates the divergence (SwitchGeometry.validateDivergence)
 * and gauge compatibility; invalid junctions are rejected. Route input is
 * server-value; committedRoute is derived.
 */
public final class SwitchNetwork {

	private long nextJunction = 1L;
	private final Map<Long, SwitchJunction> junctions = new LinkedHashMap<Long, SwitchJunction>();

	public synchronized SwitchJunction registerJunction(long nodeId,
			RailSegment mainIn, RailSegment mainOut, List<RailSegment> branches) {
		return registerJunction(nodeId, mainIn, mainOut, branches, SwitchGeometry.MAX_SWITCH_ANGLE_DEG);
	}

	public synchronized SwitchJunction registerJunction(long nodeId,
			RailSegment mainIn, RailSegment mainOut, List<RailSegment> branches, double maxAllowedDeg) {
		if (mainIn == null || mainOut == null || branches == null || branches.isEmpty()
				|| mainIn.railId() == null || mainOut.railId() == null) {
			return null;
		}
		if (mainIn.lifecycle() != RailSegment.Lifecycle.ACTIVE
				|| mainOut.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
			return null;
		}
		// Shared-node verification: mainIn.END, mainOut.START and every
		// branch.START must be at the SAME position (within coalesce
		// tolerance). A junction that joins disconnected segments is rejected.
		net.minecraft.railsys.geometry.AnchorDefinition mainInEnd = mainIn.endpointB().anchor();
		net.minecraft.railsys.geometry.AnchorDefinition mainOutStart = mainOut.endpointA().anchor();
		double nodeX = (mainInEnd.x + mainOutStart.x) / 2.0D;
		double nodeZ = (mainInEnd.z + mainOutStart.z) / 2.0D;
		if (Math.hypot(mainInEnd.x - mainOutStart.x, mainInEnd.z - mainOutStart.z)
				> net.minecraft.railsys.network.ProductionRailNetwork.NodeCoalesceTolerance) {
			return null; // mainIn.END and mainOut.START do not share a node
		}
		double mainForward = mainOutStart.yawDeg;
		for (RailSegment b : branches) {
			if (b == null || b.railId() == null || b.lifecycle() != RailSegment.Lifecycle.ACTIVE) {
				return null;
			}
			net.minecraft.railsys.geometry.AnchorDefinition bStart = b.endpointA().anchor();
			// branch.START must share the node position too.
			if (Math.hypot(bStart.x - nodeX, bStart.z - nodeZ)
					> net.minecraft.railsys.network.ProductionRailNetwork.NodeCoalesceTolerance) {
				return null;
			}
			SwitchGeometry.Validation v = SwitchGeometry.validateDivergence(
					mainForward, bStart.yawDeg, mainOut.gaugeM(), b.gaugeM(), maxAllowedDeg);
			if (!v.valid) {
				return null;
			}
		}
		SwitchJunctionId id = SwitchJunctionId.of(nextJunction++);
		SwitchJunction j = new SwitchJunction(id, nodeId, mainIn, mainOut, branches);
		junctions.put(id.value(), j);
		return j;
	}

	public synchronized SwitchJunction junction(SwitchJunctionId id) {
		return id == null ? null : junctions.get(id.value());
	}

	public synchronized List<SwitchJunction> junctions() {
		return Collections.unmodifiableList(new ArrayList<SwitchJunction>(junctions.values()));
	}

	public synchronized int junctionCount() {
		return junctions.size();
	}

	/** Set route input (server value); returns true when accepted. */
	public synchronized boolean setRouteInput(SwitchJunctionId id, SwitchRoute route, int branchIndex) {
		SwitchJunction j = junction(id);
		if (j == null) {
			return false;
		}
		j.setRouteInput(route, branchIndex);
		return true;
	}

	public synchronized SwitchRoute committedRoute(SwitchJunctionId id) {
		SwitchJunction j = junction(id);
		return j == null ? SwitchRoute.UNKNOWN : j.committedRoute();
	}

	/** Remove a junction — retire, never reuse id. */
	public synchronized boolean removeJunction(SwitchJunctionId id) {
		SwitchJunction j = junctions.remove(id == null ? -1L : id.value());
		if (j == null) {
			return false;
		}
		j.retire();
		return true;
	}

	public synchronized void clear() {
		for (SwitchJunction j : junctions.values()) {
			j.retire();
		}
		junctions.clear();
	}
}
