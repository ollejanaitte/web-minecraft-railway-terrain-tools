package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.railsys.placement.RailsysPlacementController;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * ItemRailsysMarkerWand — Phase 1-R6/R7 right-click marker placement UX.
 *
 * In a NORMAL world (R7):
 *   - Right-click a block: select POS1 (Marker A) then POS2 (Marker B).
 *     Once both markers are set, the preview path is auto-built from the
 *     production AnchorDefinition -> RailPath.fromMarkers pipeline (client
 *     side, so arrows + preview render immediately).
 *   - Sneak + right-click a block: if a preview exists -> CONFIRM (promote to
 *     production rail); otherwise -> CLEAR markers.
 *
 * The item runs on BOTH the client (prediction path that drives the client-side
 * RailsysPlacementState/RailsysRenderManager statics used by the renderer) and
 * the server (mirror). Client-side chat is used for feedback so the message
 * appears even in single-player Web Worker setups.
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
				RailsysPlacementController.confirmOrClear(entityplayer);
			}
			return true;
		}
		if (world.isRemote) {
			RailsysPlacementController.select(entityplayer, blockpos);
		}
		return true;
	}
}
