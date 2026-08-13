package net.minecraft.railsys.placement;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.railsys.modelpack.RailsysInternalAsset;
import net.minecraft.railsys.render.RailsysModelPackClient;

/**
 * RailsysAssetSelector — Phase 1-R15 Rail Asset Selector GUI (Railsys-native
 * UI, NOT an RTM GUI copy).
 *
 * Opened by Shift + Right-click on air while holding the marker wand. Lists
 * the ModelPack assets (pack + rail asset + compatibility + current selection)
 * and lets the player pick the current rail asset with a single click. The
 * selection changes APPEARANCE only — geometry (RailPath, endpoints, length,
 * gauge, cant) is never modified (R10F F4 / R13 / R14).
 */
public class RailsysAssetSelector extends GuiScreen {

	private static final int PAGE_SIZE = 12;

	private int page = 0;
	private int totalPages = 1;

	@Override
	public void initGui() {
		this.buttonList.clear();
		int n = RailsysModelPackClient.assetCount();
		totalPages = Math.max(1, (n + PAGE_SIZE - 1) / PAGE_SIZE);
		int left = 10;
		int y = 24;
		List<String> ids = RailsysModelPackClient.assetIds();
		int start = page * PAGE_SIZE;
		for (int i = start; i < ids.size() && i < start + PAGE_SIZE; i++) {
			String id = ids.get(i);
			RailsysInternalAsset a = RailsysModelPackClient.asset(id);
			String label = id + "  [" + (a == null ? "?" : a.compatibility) + "]";
			if (a != null && a.ballastBlock != null && !a.ballastBlock.isEmpty()) {
				label += "  ballast=" + a.ballastBlock;
			}
			if (a != null && !a.movableComponents.isEmpty()) {
				label += "  (switch parts)";
			}
			this.buttonList.add(new GuiButton(i - start + 100, left, y, this.width - 20, 14, label));
			y += 16;
		}
		if (page > 0) {
			this.buttonList.add(new GuiButton(998, left, y, 48, 14, "< Prev"));
		}
		if (page < totalPages - 1) {
			this.buttonList.add(new GuiButton(999, this.width - 58, y, 48, 14, "Next >"));
		}
		this.buttonList.add(new GuiButton(0, left, this.height - 22, this.width - 20, 16,
				"Current: " + RailsysModelPackClient.currentAssetId() + "   (Close)"));
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 998) {
			page = Math.max(0, page - 1);
			initGui();
		} else if (button.id == 999) {
			page = Math.min(totalPages - 1, page + 1);
			initGui();
		} else if (button.id == 0) {
			this.mc.displayGuiScreen(null);
		} else if (button.id >= 100) {
			int idx = button.id - 100 + page * PAGE_SIZE;
			List<String> ids = RailsysModelPackClient.assetIds();
			if (idx >= 0 && idx < ids.size()) {
				RailsysModelPackClient.setCurrentAsset(ids.get(idx));
				if (this.mc.thePlayer != null) {
					this.mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
							"railsys15: selected " + RailsysModelPackClient.currentAssetId()));
				}
				initGui(); // refresh current label
			}
		}
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		this.drawDefaultBackground();
		drawString(this.fontRendererObj, "Railsys Rail Asset Selector  (Shift+Right-click)  — pack "
				+ RailsysModelPackClient.packIds() + " — page " + (page + 1) + "/" + totalPages,
				8, 8, 0xFFFFFF);
		super.drawScreen(par1, par2, par3);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	/** Open the selector for a player (client thread). */
	public static void open(EntityPlayer player) {
		RailsysModelPackClient.ensureInitialized();
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null) {
			mc.displayGuiScreen(new RailsysAssetSelector());
		}
	}

	/** True when the player holds the Railsys marker wand (selection UX gate). */
	public static boolean isRailsysWandHeld(EntityPlayer player) {
		if (player == null) {
			return false;
		}
		ItemStack stack = player.getHeldItem();
		return stack != null && stack.getItem() == Items.railsys_marker_wand;
	}
}
