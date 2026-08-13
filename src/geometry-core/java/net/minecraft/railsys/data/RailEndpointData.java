package net.minecraft.railsys.data;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;

/**
 * RailEndpointData — the PRODUCTION endpoint of a confirmed rail.
 *
 * R12 Production Rail Data Model §4.2 (frozen):
 *   - anchor (AnchorDefinition) is AUTHORITATIVE (F1/F2 semantics, support
 *     surface datum, position/direction/handle single source of truth);
 *   - markerType / placement are AUTHORITATIVE placement metadata
 *     (REQ-P1-01; R13 uses NORMAL / CENTER);
 *   - blockPos is DERIVED metadata (CC-4) for future spatial tie-in — never
 *     authoritative.
 */
public final class RailEndpointData {

	public enum MarkerType {
		NORMAL, JUNCTION
	}

	public enum Placement {
		CENTER, EDGE
	}

	private final AnchorDefinition anchor;
	private final MarkerType markerType;
	private final Placement placement;

	public RailEndpointData(AnchorDefinition anchor, MarkerType markerType, Placement placement) {
		if (anchor == null) {
			throw new IllegalArgumentException("RailEndpointData requires an anchor");
		}
		RailMath.requireFinite(anchor.x, "anchor.x");
		RailMath.requireFinite(anchor.y, "anchor.y");
		RailMath.requireFinite(anchor.z, "anchor.z");
		RailMath.requireFinite(anchor.yawDeg, "anchor.yawDeg");
		RailMath.requireFinite(anchor.pitchDeg, "anchor.pitchDeg");
		this.anchor = anchor;
		this.markerType = markerType == null ? MarkerType.NORMAL : markerType;
		this.placement = placement == null ? Placement.CENTER : placement;
	}

	public AnchorDefinition anchor() {
		return this.anchor;
	}

	public MarkerType markerType() {
		return this.markerType;
	}

	public Placement placement() {
		return this.placement;
	}

	/** DERIVED block origin (CC-4): floor of the anchor coordinate. NOT authoritative. */
	public int blockX() {
		return (int) Math.floor(this.anchor.x);
	}

	/** DERIVED block origin (CC-4): floor of the support-surface anchor y. NOT authoritative. */
	public int blockY() {
		return (int) Math.floor(this.anchor.y);
	}

	/** DERIVED block origin (CC-4). NOT authoritative. */
	public int blockZ() {
		return (int) Math.floor(this.anchor.z);
	}

	@Override
	public String toString() {
		return "RailEndpointData{anchor=(" + anchor.x + "," + anchor.y + "," + anchor.z
				+ ") yaw=" + anchor.yawDeg + " pitch=" + anchor.pitchDeg
				+ " type=" + markerType + " placement=" + placement + "}";
	}
}
