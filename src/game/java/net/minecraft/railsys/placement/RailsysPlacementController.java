package net.minecraft.railsys.placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.render.RailsysRenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

/**
 * RailsysPlacementController — Phase 1-R7/R8 client-side placement workflow.
 *
 * Drives the ACTUAL Marker Rail Placement UX in a normal world, entirely on the
 * CLIENT thread (RailsysPlacementState / RailsysRenderManager are client-side
 * statics; Web Worker separation means server-side commands cannot reach the
 * client renderer). The SAME code path is used by the marker wand item and the
 * client edit commands, so every UX action updates the production geometry:
 *
 *   select block            -> POS1 (then POS2) marker + auto preview
 *   confirm                 -> promote preview to confirmed + production render
 *   clear / cancel          -> reset markers / preview
 *   edit (R8)               -> rotate POS1/POS2 yaw, handle, pitch, cant
 *                              -> rebuild preview geometry via RailPath.fromMarkers
 *
 * Preview and confirmed rail are built through the SAME
 * AnchorDefinition -> RailPath.fromMarkers -> RailPath pipeline; no
 * renderer-only fake preview. Preview and confirmed are the SAME RailPath
 * object (promoted), so they are numerically identical by construction.
 */
public final class RailsysPlacementController {

	public static final int PLACEMENT_PIECE_ID = 8001;

	private RailsysPlacementController() {
	}

	/** Right-click a block: select POS1 then POS2; auto-preview when both set. */
	public static boolean select(EntityPlayer player, BlockPos pos) {
		if (player == null || pos == null) {
			return false;
		}
		boolean ok = RailsysMarkerSelection.select(player, pos);
		rebuildPreview(player);
		return ok;
	}

	/** Build (or clear) the preview path from the current markers + cant. */
	public static void rebuildPreview(EntityPlayer player) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (st.hasMarkerA() && st.hasMarkerB()) {
			try {
				RailPath path = RailPath.fromMarkers(st.getMarkerA(), st.getMarkerB(),
						st.getCantDeg(), PLACEMENT_PIECE_ID);
				st.setPreviewPath(path);
				if (player != null) {
					player.addChatMessage(new ChatComponentText("railsys: preview ready (length "
							+ String.format("%.2f", path.totalLength()) + "m, cant="
							+ String.format("%.1f", st.getCantDeg()) + "deg)"));
				}
			} catch (RuntimeException e) {
				st.clearPreview();
				if (player != null) {
					player.addChatMessage(new ChatComponentText("railsys: preview failed: " + e.getMessage()));
				}
			}
		} else {
			st.clearPreview();
		}
	}

	/** Confirm: promote the current preview to a production rail (same RailPath). */
	public static boolean confirm(EntityPlayer player) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (!st.hasPreview()) {
			if (player != null) {
				player.addChatMessage(new ChatComponentText("railsys: no preview to confirm"));
			}
			return false;
		}
		st.confirm();
		// After confirm, the preview is promoted to the production rail — clear
		// the preview overlay so only the confirmed continuous rail (in the active
		// asset appearance) is rendered.
		st.clearPreview();
		RailsysRenderManager.setRenderPath(st.getConfirmedPath());
		if (player != null) {
			player.addChatMessage(new ChatComponentText("railsys: confirmed (length "
					+ String.format("%.2f", st.getConfirmedPath().totalLength()) + "m)"));
		}
		return true;
	}

	/** Clear markers, preview and confirmed rail. */
	public static void clear(EntityPlayer player) {
		RailsysPlacementState.getInstance().cancel();
		RailsysRenderManager.clear();
		if (player != null) {
			player.addChatMessage(new ChatComponentText("railsys: cleared"));
		}
	}

	/** Sneak+right-click block: confirm if a preview exists, otherwise clear. */
	public static void confirmOrClear(EntityPlayer player) {
		if (RailsysPlacementState.getInstance().hasPreview()) {
			confirm(player);
		} else {
			clear(player);
		}
	}

	/** Cancel preview but keep markers (re-selectable). */
	public static void cancelPreview(EntityPlayer player) {
		RailsysPlacementState.getInstance().clearPreview();
		if (player != null) {
			player.addChatMessage(new ChatComponentText("railsys: preview cancelled"));
		}
	}

	// ================= R8 Anchor Editing =================

	/** Rotate the POS1 (Marker A) yaw by deltaDeg. */
	public static boolean rotatePos1(EntityPlayer player, double deltaDeg) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (!st.hasMarkerA()) {
			msg(player, "railsys: set POS1 first");
			return false;
		}
		AnchorDefinition a = st.getMarkerA();
		st.setMarkerA(new AnchorDefinition(a.x, a.y, a.z, RailMath.wrapYaw(a.yawDeg + deltaDeg), a.pitchDeg,
				a.lengthH_m, a.lengthV_m));
		rebuildPreview(player);
		return true;
	}

	/** Rotate the POS2 (Marker B) yaw by deltaDeg. */
	public static boolean rotatePos2(EntityPlayer player, double deltaDeg) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (!st.hasMarkerB()) {
			msg(player, "railsys: set POS2 first");
			return false;
		}
		AnchorDefinition b = st.getMarkerB();
		st.setMarkerB(new AnchorDefinition(b.x, b.y, b.z, RailMath.wrapYaw(b.yawDeg + deltaDeg), b.pitchDeg,
				b.lengthH_m, b.lengthV_m));
		rebuildPreview(player);
		return true;
	}

	/** Set the handle length (curve strength) of both anchors. */
	public static boolean setHandle(EntityPlayer player, double handle) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (handle < 0.1D || handle > 20.0D) {
			msg(player, "railsys: handle must be in [0.1, 20.0]");
			return false;
		}
		boolean changed = false;
		if (st.hasMarkerA()) {
			AnchorDefinition a = st.getMarkerA();
			st.setMarkerA(new AnchorDefinition(a.x, a.y, a.z, a.yawDeg, a.pitchDeg, handle, a.lengthV_m));
			changed = true;
		}
		if (st.hasMarkerB()) {
			AnchorDefinition b = st.getMarkerB();
			st.setMarkerB(new AnchorDefinition(b.x, b.y, b.z, b.yawDeg, b.pitchDeg, handle, b.lengthV_m));
			changed = true;
		}
		if (changed) {
			rebuildPreview(player);
		} else {
			msg(player, "railsys: set POS1/POS2 first");
		}
		return changed;
	}

	/** Set the gradient pitch (degrees, positive = up) on both anchors. */
	public static boolean setPitch(EntityPlayer player, double pitchDeg) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (pitchDeg < -45.0D || pitchDeg > 45.0D) {
			msg(player, "railsys: pitch must be in [-45, 45]");
			return false;
		}
		boolean changed = false;
		if (st.hasMarkerA()) {
			AnchorDefinition a = st.getMarkerA();
			st.setMarkerA(new AnchorDefinition(a.x, a.y, a.z, a.yawDeg, pitchDeg, a.lengthH_m, a.lengthV_m));
			changed = true;
		}
		if (st.hasMarkerB()) {
			AnchorDefinition b = st.getMarkerB();
			st.setMarkerB(new AnchorDefinition(b.x, b.y, b.z, b.yawDeg, pitchDeg, b.lengthH_m, b.lengthV_m));
			changed = true;
		}
		if (changed) {
			rebuildPreview(player);
		} else {
			msg(player, "railsys: set POS1/POS2 first");
		}
		return changed;
	}

	/** Set the cant (roll, degrees; positive = right rail lower). */
	public static boolean setCant(EntityPlayer player, double cantDeg) {
		if (cantDeg < -45.0D || cantDeg > 45.0D) {
			msg(player, "railsys: cant must be in [-45, 45]");
			return false;
		}
		RailsysPlacementState.getInstance().setCantDeg(cantDeg);
		rebuildPreview(player);
		return true;
	}

	private static void msg(EntityPlayer player, String text) {
		if (player != null) {
			player.addChatMessage(new ChatComponentText(text));
		}
	}
}
