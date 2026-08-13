package net.minecraft.railsys.junction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.railsys.data.RailSegment;

/**
 * SwitchJunction — a production switch/turnout at a network node (R17).
 *
 * A junction joins a main-in segment (its END is at the node), a main-out
 * segment (its START is at the node, near-straight continuation of main-in),
 * and one or more branch segments (each START at the node, diverging by a
 * switch angle). The junction commits to a {@link SwitchRoute} derived from
 * the route input; vehicles resolve which segment to continue onto.
 */
public final class SwitchJunction {

	public enum Lifecycle {
		ACTIVE, RETIRED
	}

	private final SwitchJunctionId junctionId;
	private final long nodeId;
	private final RailSegment mainIn;
	private final RailSegment mainOut;
	private final List<RailSegment> branches = new ArrayList<RailSegment>();
	private SwitchRoute routeInput = SwitchRoute.UNKNOWN;
	private int committedBranchIndex = -1;
	private Lifecycle lifecycle = Lifecycle.ACTIVE;

	SwitchJunction(SwitchJunctionId id, long nodeId, RailSegment mainIn, RailSegment mainOut,
			List<RailSegment> branches) {
		this.junctionId = id;
		this.nodeId = nodeId;
		this.mainIn = mainIn;
		this.mainOut = mainOut;
		if (branches != null) {
			this.branches.addAll(branches);
		}
	}

	public SwitchJunctionId junctionId() {
		return this.junctionId;
	}

	public long nodeId() {
		return this.nodeId;
	}

	public RailSegment mainIn() {
		return this.mainIn;
	}

	public RailSegment mainOut() {
		return this.mainOut;
	}

	/** Unmodifiable branch segments. */
	public List<RailSegment> branches() {
		return Collections.unmodifiableList(new ArrayList<RailSegment>(this.branches));
	}

	public int branchCount() {
		return this.branches.size();
	}

	public Lifecycle lifecycle() {
		return this.lifecycle;
	}

	void retire() {
		this.lifecycle = Lifecycle.RETIRED;
	}

	// ---------------- Route ----------------

	public SwitchRoute routeInput() {
		return this.routeInput;
	}

	/**
	 * Set the route input (server value). BRANCH requires a valid branch index;
	 * invalid -> UNKNOWN (no silent wrong route).
	 */
	public void setRouteInput(SwitchRoute route, int branchIndex) {
		if (lifecycle != Lifecycle.ACTIVE) {
			this.routeInput = SwitchRoute.UNKNOWN;
			this.committedBranchIndex = -1;
			return;
		}
		if (route == SwitchRoute.BRANCH) {
			if (branchIndex >= 0 && branchIndex < this.branches.size()) {
				this.routeInput = SwitchRoute.BRANCH;
				this.committedBranchIndex = branchIndex;
			} else {
				this.routeInput = SwitchRoute.UNKNOWN;
				this.committedBranchIndex = -1;
			}
		} else if (route == SwitchRoute.THROUGH) {
			this.routeInput = SwitchRoute.THROUGH;
			this.committedBranchIndex = -1;
		} else {
			this.routeInput = SwitchRoute.UNKNOWN;
			this.committedBranchIndex = -1;
		}
	}

	/** Index of the committed branch (valid only when committedRoute==BRANCH). */
	public int committedBranchIndex() {
		return this.committedBranchIndex;
	}

	/** The DERIVED authoritative route. */
	public SwitchRoute committedRoute() {
		if (lifecycle != Lifecycle.ACTIVE) {
			return SwitchRoute.UNKNOWN;
		}
		return this.routeInput;
	}

	/** The committed next segment (mainOut for THROUGH, a branch for BRANCH). */
	public RailSegment committedNext() {
		SwitchRoute r = committedRoute();
		if (r == SwitchRoute.THROUGH) {
			return this.mainOut;
		}
		if (r == SwitchRoute.BRANCH && this.committedBranchIndex >= 0
				&& this.committedBranchIndex < this.branches.size()) {
			return this.branches.get(this.committedBranchIndex);
		}
		return null;
	}

	/**
	 * Resolve the next segment for a vehicle leaving {@code fromSegment} at
	 * this junction. A branch returns to the main-out (branch end -> main).
	 */
	public RailSegment resolveRoute(RailSegment fromSegment) {
		if (fromSegment == null || lifecycle != Lifecycle.ACTIVE) {
			return null;
		}
		if (fromSegment == this.mainIn) {
			return committedNext();
		}
		if (this.branches.contains(fromSegment)) {
			return this.mainOut;
		}
		return null;
	}

	@Override
	public String toString() {
		return "SwitchJunction{" + junctionId + " node=" + nodeId + " mainIn=" + mainIn.railId()
				+ " mainOut=" + mainOut.railId() + " branches=" + branches.size()
				+ " route=" + committedRoute() + "}";
	}
}
