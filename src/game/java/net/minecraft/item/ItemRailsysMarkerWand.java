package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.placement.RailsysAssetSelector;
import net.minecraft.railsys.placement.RailsysPlacementController;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * ItemRailsysMarkerWand — Phase 1-R6/R7/R10 right-click marker placement UX.
 *
 * In a NORMAL world:
 *   - Right-click a block: select POS1 (Marker A) then POS2 (Marker B).
 *     Once both markers are set, the preview path is auto-built from the
 *     production AnchorDefinition -> RailPath.fromMarkers pipeline (client
 *     side, so arrows + preview render immediately).
 *   - Sneak + right-click: CONFIRM ONLY via RailsysPlacementController.confirm.
 *     If there is no valid preview this is an error and the state does not
 *     change. Sneak + right-click NEVER clears markers or a confirmed rail.
 *
 * The item runs on BOTH the client (prediction path that drives the client-side
 * RailsysPlacementState/RailsysRenderManager statics used by the renderer) and
 * the server (mirror). Client-side chat is used for feedback so the message
 * appears even in single-player Web Worker setups.
 *
 * Phase 1-R10 clicked-surface contract: Minecraft reports the clicked BLOCK
 * (bottom Y) plus the hit face, while the production RailPath treats the anchor
 * Y as the SUPPORT surface. The wand therefore forwards the hit EnumFacing to
 * RailsysPlacementController.selectOnFace, which converts a top-face click to
 * the support surface (Y+1) and rejects any non-UP face.
 */
public class ItemRailsysMarkerWand extends Item {

	public ItemRailsysMarkerWand() {
		this.setMaxStackSize(1);
		this.setCreativeTab(CreativeTabs.tabTools);
	}

	@Override
	public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, BlockPos blockpos,
			EnumFacing enumfacing, float hitX, float hitY, float hitZ) {
		if (entityplayer == null) {
			return true;
		}
		if (entityplayer.isSneaking()) {
			if (world.isRemote) {
				RailsysPlacementController.confirm(entityplayer);
			}
			return true;
		}
		if (world.isRemote) {
			RailsysPlacementController.selectOnFace(entityplayer, blockpos, enumfacing);
		}
		return true;
	}

	/**
	 * Phase 1-R15: Shift + Right-click on AIR opens the Railsys ModelPack
	 * Asset Selector (Rail Asset Selection UX). Appearance-only; the RailPath
	 * is never changed. Block clicks keep the R7/R10 confirm contract.
	 */
	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer) {
		if (entityplayer != null && entityplayer.isSneaking() && world.isRemote) {
			RailsysAssetSelector.open(entityplayer);
		}
		return itemstack;
	}
}
