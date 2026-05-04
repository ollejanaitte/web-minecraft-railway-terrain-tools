package net.minecraft.client.gui;

public class GuiWorldEditTest extends GuiScreen {

	public void initGui() {
		System.out.println("WEUI TEST: initGui called");
		this.buttonList.clear();
		this.buttonList.add(new GuiButton(0, this.width / 2 - 50, this.height / 2 + 30, 100, 20, "Close"));
	}

	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) {
			this.mc.displayGuiScreen(null);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Hello World", this.width / 2, this.height / 2 - 20, 0xFFFFFF);
		this.drawCenteredString(this.fontRendererObj, "WorldEdit UI Test", this.width / 2, this.height / 2, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
