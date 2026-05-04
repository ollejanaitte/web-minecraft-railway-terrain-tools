package net.minecraft.client.gui;

public class GuiWorldEdit extends GuiScreen {

	private int step = 5;

	public void initGui() {
		this.buttonList.clear();

		int buttonWidth = this.width < 330 ? 90 : 100;
		int buttonHeight = this.height < 300 ? 18 : 20;
		int gap = 5;
		int yStep = this.height < 300 ? 20 : 24;
		int columns = this.width < 295 ? 2 : 3;
		int startX = this.width / 2 - (columns * buttonWidth + (columns - 1) * gap) / 2;
		int y = Math.max(82, this.height / 2 - 104);
		int index = 0;

		addGridButton(1, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Pos1");
		addGridButton(2, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Pos2");
		addGridButton(3, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Copy");
		addGridButton(4, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Paste");
		addGridButton(5, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Preview X+");
		addGridButton(6, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Preview X-");
		addGridButton(7, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Preview Z+");
		addGridButton(8, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Preview Z-");
		addGridButton(9, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Paste Preview");
		addGridButton(10, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Clear Preview");
		addGridButton(11, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Undo");
		addGridButton(12, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Desel");
		addGridButton(13, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Step 1");
		addGridButton(14, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Step 5");
		addGridButton(15, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Step 10");
		addGridButton(16, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Offset X+");
		addGridButton(17, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Offset X-");
		addGridButton(18, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Offset Z+");
		addGridButton(19, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Offset Z-");
		addGridButton(20, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Offset Reset");
		addGridButton(21, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Stack X");
		addGridButton(22, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Stack Z");
		addGridButton(23, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Fill Stone");
		addGridButton(24, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Fill Held/Look");
		addGridButton(25, startX, y, buttonWidth, buttonHeight, gap, columns, index++, "Clear");

		int rows = (index + columns - 1) / columns;
		int closeY = Math.min(this.height - 28, y + rows * yStep + 8);
		this.buttonList.add(new GuiButton(0, this.width / 2 - 50, closeY, 100, 20, "Close"));
	}

	private void addButton(int id, int x, int y, int width, int height, String text) {
		this.buttonList.add(new GuiButton(id, x, y, width, height, text));
	}

	private void addGridButton(int id, int startX, int startY, int width, int height, int gap, int columns, int index,
			String text) {
		int col = index % columns;
		int row = index / columns;
		int yStep = this.height < 300 ? 20 : 24;
		addButton(id, startX + col * (width + gap), startY + row * yStep, width, height, text);
	}

	protected void actionPerformed(GuiButton button) {
		if (!button.enabled) {
			return;
		}

		switch (button.id) {
		case 0:
			this.mc.displayGuiScreen(null);
			return;
		case 1:
			sendCommand("/pos1");
			return;
		case 2:
			sendCommand("/pos2");
			return;
		case 3:
			sendCommand("/copy");
			return;
		case 4:
			sendCommand("/paste");
			return;
		case 5:
			sendCommand("/preview x " + this.step);
			return;
		case 6:
			sendCommand("/preview x -" + this.step);
			return;
		case 7:
			sendCommand("/preview z " + this.step);
			return;
		case 8:
			sendCommand("/preview z -" + this.step);
			return;
		case 9:
			sendCommand("/pastepreview");
			return;
		case 10:
			sendCommand("/previewclear");
			return;
		case 11:
			sendCommand("/undo");
			return;
		case 12:
			sendCommand("/desel");
			return;
		case 13:
			this.step = 1;
			return;
		case 14:
			this.step = 5;
			return;
		case 15:
			this.step = 10;
			return;
		case 16:
			sendCommand("/offset x " + this.step);
			return;
		case 17:
			sendCommand("/offset x -" + this.step);
			return;
		case 18:
			sendCommand("/offset z " + this.step);
			return;
		case 19:
			sendCommand("/offset z -" + this.step);
			return;
		case 20:
			sendCommand("/offsetreset");
			return;
		case 21:
			sendCommand("/stack x 2");
			return;
		case 22:
			sendCommand("/stack z 2");
			return;
		case 23:
			sendCommand("/fill stone");
			return;
		case 24:
			sendCommand("/fill");
			return;
		case 25:
			sendCommand("/clear");
			return;
		default:
			return;
		}
	}

	private void sendCommand(String command) {
		if (this.mc.thePlayer != null) {
			this.mc.thePlayer.sendChatMessage(command);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "WorldEdit Control Panel", this.width / 2, 24, 0xFFFFFF);
		this.drawCenteredString(this.fontRendererObj, "Step: " + this.step, this.width / 2, 38, 0xDDDDDD);
		this.drawCenteredString(this.fontRendererObj, "/wand: left click pos1, right click pos2", this.width / 2, 50,
				0xAAAAAA);
		this.drawCenteredString(this.fontRendererObj, "Preview buttons use current step", this.width / 2, 62, 0xAAAAAA);
		this.drawCenteredString(this.fontRendererObj, "Commands are sent as chat commands", this.width / 2, 74,
				0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}
