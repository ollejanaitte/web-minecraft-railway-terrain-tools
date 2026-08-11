package net.minecraft.railsys.placement;

import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.path.RailPath;

/**
 * RailsysPlacementState — client-side placement workflow state.
 *
 * Phase 1.4 Marker Placement: Marker A -> Marker B -> Preview -> Confirm.
 * Holds the two anchors (position + yaw/pitch + handle length). The preview
 * and confirmed RailPath are both produced from the SAME AnchorDefinition ->
 * Geometry -> RailPiece -> RailPath pipeline (no placement-specific geometry).
 */
public final class RailsysPlacementState {

	public static final double DEFAULT_HANDLE_M = 1.0D;
	public static final double DEFAULT_PITCH_DEG = 0.0D;

	private static final RailsysPlacementState INSTANCE = new RailsysPlacementState();

	private AnchorDefinition markerA;
	private AnchorDefinition markerB;
	private RailPath previewPath;
	private RailPath confirmedPath;

	private RailsysPlacementState() {
	}

	public static RailsysPlacementState getInstance() {
		return INSTANCE;
	}

	public void setMarkerA(AnchorDefinition a) {
		this.markerA = a;
	}

	public void setMarkerB(AnchorDefinition b) {
		this.markerB = b;
	}

	public AnchorDefinition getMarkerA() {
		return this.markerA;
	}

	public AnchorDefinition getMarkerB() {
		return this.markerB;
	}

	public boolean hasMarkerA() {
		return this.markerA != null;
	}

	public boolean hasMarkerB() {
		return this.markerB != null;
	}

	public void setPreviewPath(RailPath path) {
		this.previewPath = path;
	}

	public RailPath getPreviewPath() {
		return this.previewPath;
	}

	public boolean hasPreview() {
		return this.previewPath != null && this.previewPath.entryCount() > 0;
	}

	/** Confirm: promote preview to production render path (replaces any). */
	public void confirm() {
		if (hasPreview()) {
			this.confirmedPath = this.previewPath;
		}
	}

	public RailPath getConfirmedPath() {
		return this.confirmedPath;
	}

	public boolean hasConfirmed() {
		return this.confirmedPath != null && this.confirmedPath.entryCount() > 0;
	}

	public void cancel() {
		this.markerA = null;
		this.markerB = null;
		this.previewPath = null;
		this.confirmedPath = null;
	}

	public void clearPreview() {
		this.previewPath = null;
	}
}
