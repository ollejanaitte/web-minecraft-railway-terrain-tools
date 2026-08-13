package net.minecraft.railsys.placement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.geometry.AnchorDefinition;
import net.minecraft.railsys.geometry.RailMath;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.render.RailsysRenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;

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
 *   confirm                 -> promote preview to confirmed + production render,
 *                              then tidy the transient markers/preview
 *   clear                   -> reset transient session (markers/preview/cant);
 *                              the confirmed rail and active asset are preserved
 *   cancel (preview)        -> discard preview ONLY; markers/edit/cant kept
 *   edit (R8)               -> rotate POS1/POS2 yaw, handle, pitch, cant
 *                              -> rebuild preview geometry via RailPath.fromMarkers
 *
 * Sneak+right-click with the wand confirms ONLY (never clears); if there is no
 * preview, confirm is an error and the state does not change.
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

	/**
	 * Right-click a block FACE (marker wand path): the actual clicked block
	 * surface, not a canonical coordinate. Delegates to
	 * RailsysMarkerSelection.selectOnFace, which converts a top-face click to
	 * the support surface (anchor y = clicked block y + 1) and REJECTS any
	 * non-UP face with clear chat and no marker/preview state mutation
	 * (Phase 1 horizontal rail placement contract). The preview is rebuilt
	 * ONLY on a successful select so a rejected face never mutates state.
	 */
	public static boolean selectOnFace(EntityPlayer player, BlockPos pos, EnumFacing face) {
		if (player == null || pos == null || face == null) {
			return false;
		}
		boolean ok = RailsysMarkerSelection.selectOnFace(player, pos, face);
		if (ok) {
			rebuildPreview(player);
		}
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

	/**
	 * Confirm: promote the exact preview RailPath to confirmed, set the
	 * production render path, then tidy the transient markers/preview so the
	 * arrow overlays and edit handles are tidied. Confirmed anchors + asset
	 * metadata are preserved by RailsysPlacementState.
	 *
	 * R13: production registration happens FIRST (server side of the handoff).
	 * Only a VALID production RailSegment (stable id issued, rail-level
	 * validation passed) is allowed to promote the exact preview — an invalid
	 * confirm is rejected and NO client-confirmed state is produced (no stable
	 * id, no promotion). This is the R12 §3.1 accept-or-reject rule.
	 */
	public static boolean confirm(EntityPlayer player) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		if (!st.hasPreview()) {
			if (player != null) {
				player.addChatMessage(new ChatComponentText("railsys: no preview to confirm"));
			}
			return false;
		}
		// R13: validate + issue stable id FIRST (authoritative side).
		String assetId = net.minecraft.railsys.render.RailsysRenderManager.getActiveAssetId();
		double gauge = net.minecraft.railsys.placement.RailsysProductionRailStore.clampGaugeForDefaults(
				net.minecraft.railsys.render.RailsysRenderManager.getActiveAsset().gaugeM);
		RailPath preview = st.getPreviewPath();
		net.minecraft.railsys.data.RailSegment prod = net.minecraft.railsys.placement.RailsysProductionRailStore
				.getInstance()
				.confirmPreview(st.getMarkerA(), st.getMarkerB(), st.getCantDeg(), gauge, assetId, 1, preview);
		if (prod == null) {
			// Invalid confirm: reject WITHOUT promoting / clearing / issuing a
			// committed id.
			if (player != null) {
				player.addChatMessage(new ChatComponentText(
						"railsys: confirm rejected — rail fails production validation"));
			}
			return false;
		}
		// Accept: promote the exact preview (R10F F2.4) and tidy the session.
		st.confirm();
		st.clearTransientSession();
		RailsysRenderManager.setRenderPath(st.getConfirmedPath());
		if (player != null) {
			player.addChatMessage(new ChatComponentText("railsys: confirmed (" + prod.railId()
					+ " length " + String.format("%.2f", st.getConfirmedPath().totalLength()) + "m)"));
		}
		return true;
	}

	/**
	 * Clear the current transient placement session (markers, preview, transient
	 * cant/edit). A CONFIRMED rail is never destroyed: when one exists the
	 * production render path is preserved/re-asserted. When there is NO confirmed
	 * rail the render manager is left UNTOUCHED — it may hold unrelated restored
	 * or validation render paths that clear must not erase.
	 */
	public static void clear(EntityPlayer player) {
		RailsysPlacementState st = RailsysPlacementState.getInstance();
		st.clearTransientSession();
		if (st.hasConfirmed()) {
			RailsysRenderManager.setRenderPath(st.getConfirmedPath());
		}
		if (player != null) {
			player.addChatMessage(new ChatComponentText(st.hasConfirmed()
					? "railsys: session cleared; confirmed rail kept"
					: "railsys: session cleared"));
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
