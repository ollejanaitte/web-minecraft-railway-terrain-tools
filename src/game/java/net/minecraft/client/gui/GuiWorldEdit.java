package net.minecraft.client.gui;

public class GuiWorldEdit extends GuiScreen {

	private static final int PANEL_LEFT = 6;
	private static final int PANEL_TOP = 8;

	private static boolean localClipboardCopied;
	private static boolean previewSet;
	private static int previewOffsetX;
	private static int previewOffsetY;
	private static int previewOffsetZ;
	private static String localJobState = "Idle";
	private static int step = 5;

	public void initGui() {
		this.buttonList.clear();

		int panelWidth = getPanelWidth();
		int controlsLeft = panelWidth + 14;
		int availableWidth = Math.max(130, this.width - controlsLeft - 6);
		int columns = availableWidth >= 230 ? 3 : 2;
		int buttonWidth = Math.max(56, Math.min(74, (availableWidth - (columns - 1) * 4) / columns));
		int buttonHeight = this.height < 300 ? 14 : 16;
		int gap = 4;
		int left = this.width < 300 ? PANEL_LEFT + 8 : controlsLeft;
		int y = this.height < 300 ? 34 : 42;

		y = addSectionButtons("Selection", y, left, buttonWidth, buttonHeight, gap,
				new int[] { 30, 12 }, new String[] { "Wand", "Desel" });
		y = addSectionButtons("Clipboard", y, left, buttonWidth, buttonHeight, gap,
				new int[] { 3, 4, 11 }, new String[] { "Copy", "Paste", "Undo" });
		y = addSectionButtons("Preview", y, left, buttonWidth, buttonHeight, gap,
				new int[] { 5, 6, 26, 27, 7, 8, 9, 10 }, new String[] { "Prev X+", "Prev X-", "Prev Y+", "Prev Y-",
						"Prev Z+", "Prev Z-", "Paste Prev", "Clear Prev" });
		y = addSectionButtons("Transform", y, left, buttonWidth, buttonHeight, gap,
				new int[] { 16, 17, 28, 29, 18, 19, 20, 21, 31, 22 }, new String[] { "Off X+", "Off X-", "Off Y+",
						"Off Y-", "Off Z+", "Off Z-", "Reset", "Stack X", "Stack Y", "Stack Z" });
		y = addSectionButtons("Fill", y, left, buttonWidth, buttonHeight, gap,
				new int[] { 23, 24, 25 }, new String[] { "Stone", "Held/Look", "Clear" });
		y = addSectionButtons("Step", y, left, buttonWidth, buttonHeight, gap,
				new int[] { 13, 14, 15 }, new String[] { step == 1 ? "Step: 1" : "Step 1",
						step == 5 ? "Step: 5" : "Step 5", step == 10 ? "Step: 10" : "Step 10" });

		this.buttonList.add(new GuiButton(0, left, Math.max(6, this.height - buttonHeight - 6),
				Math.min(buttonWidth * columns + gap * (columns - 1), availableWidth), buttonHeight, "Close"));
	}

	private void addButton(int id, int x, int y, int width, int height, String text) {
		this.buttonList.add(new GuiButton(id, x, y, width, height, text));
	}

	private void addGridButton(int id, int startX, int startY, int width, int height, int gap, int columns, int index,
			String text) {
		int col = index % columns;
		int row = index / columns;
		int yStep = this.height < 300 ? 20 : 23;
		addButton(id, startX + col * (width + gap), startY + row * yStep, width, height, text);
	}

	private int addSectionButtons(String title, int y, int x, int width, int height, int gap, int[] ids, String[] labels) {
		int yStep = this.height < 300 ? 15 : 18;
		int columns = this.width - x >= 230 ? 3 : 2;
		y += this.height < 300 ? 1 : 3;
		for (int i = 0; i < ids.length; ++i) {
			int col = i % columns;
			int row = i / columns;
			addButton(ids[i], x + col * (width + gap), y + row * yStep, width, height, labels[i]);
		}
		return y + ((ids.length + columns - 1) / columns) * yStep + (this.height < 300 ? 1 : 4);
	}

	protected void actionPerformed(GuiButton button) {
		if (!button.enabled) {
			return;
		}

		switch (button.id) {
		case 0:
			this.mc.displayGuiScreen(null);
			return;
		case 30:
			sendCommand("/wand");
			localJobState = "Idle";
			return;
		case 3:
			localClipboardCopied = true;
			sendCommand("/copy");
			localJobState = "Idle";
			return;
		case 4:
			sendJobCommand("/paste");
			return;
		case 5:
			setPreview(step, 0, 0);
			sendCommand("/preview x " + step);
			return;
		case 6:
			setPreview(-step, 0, 0);
			sendCommand("/preview x -" + step);
			return;
		case 7:
			setPreview(0, 0, step);
			sendCommand("/preview z " + step);
			return;
		case 8:
			setPreview(0, 0, -step);
			sendCommand("/preview z -" + step);
			return;
		case 9:
			clearPreviewLocal();
			sendJobCommand("/pastepreview");
			return;
		case 10:
			clearPreviewLocal();
			sendCommand("/previewclear");
			localJobState = "Idle";
			return;
		case 11:
			sendJobCommand("/undo");
			return;
		case 12:
			clearPreviewLocal();
			sendCommand("/desel");
			localJobState = "Idle";
			return;
		case 13:
			step = 1;
			localJobState = "Idle";
			this.initGui();
			return;
		case 14:
			step = 5;
			localJobState = "Idle";
			this.initGui();
			return;
		case 15:
			step = 10;
			localJobState = "Idle";
			this.initGui();
			return;
		case 16:
			addPreviewOffset(step, 0, 0);
			sendCommand("/offset x " + step);
			return;
		case 17:
			addPreviewOffset(-step, 0, 0);
			sendCommand("/offset x -" + step);
			return;
		case 18:
			addPreviewOffset(0, 0, step);
			sendCommand("/offset z " + step);
			return;
		case 19:
			addPreviewOffset(0, 0, -step);
			sendCommand("/offset z -" + step);
			return;
		case 20:
			setPreview(0, 0, 0);
			sendCommand("/offsetreset");
			return;
		case 21:
			sendJobCommand("/stack x 2");
			return;
		case 22:
			sendJobCommand("/stack z 2");
			return;
		case 23:
			sendJobCommand("/fill stone");
			return;
		case 24:
			sendJobCommand("/fill");
			return;
		case 25:
			sendJobCommand("/clear");
			return;
		case 26:
			setPreview(0, step, 0);
			sendCommand("/preview y " + step);
			return;
		case 27:
			setPreview(0, -step, 0);
			sendCommand("/preview y -" + step);
			return;
		case 28:
			addPreviewOffset(0, step, 0);
			sendCommand("/offset y " + step);
			return;
		case 29:
			addPreviewOffset(0, -step, 0);
			sendCommand("/offset y -" + step);
			return;
		case 31:
			sendJobCommand("/stack y 2");
			return;
		default:
			return;
		}
	}

	private void sendJobCommand(String command) {
		localJobState = "Sent";
		sendCommand(command);
	}

	private void sendCommand(String command) {
		if (this.mc.thePlayer != null) {
			this.mc.thePlayer.sendChatMessage(command);
		}
	}

	private void setPreview(int x, int y, int z) {
		previewSet = true;
		previewOffsetX = x;
		previewOffsetY = y;
		previewOffsetZ = z;
		localJobState = "Idle";
	}

	private void addPreviewOffset(int x, int y, int z) {
		previewSet = true;
		previewOffsetX += x;
		previewOffsetY += y;
		previewOffsetZ += z;
		localJobState = "Idle";
	}

	private void clearPreviewLocal() {
		previewSet = false;
		previewOffsetX = 0;
		previewOffsetY = 0;
		previewOffsetZ = 0;
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawPanel();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawPanel() {
		int panelWidth = getPanelWidth();
		int panelLeft = PANEL_LEFT;
		int panelTop = PANEL_TOP;
		int panelBottom = this.height - 6;
		drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xAA000000);
		drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 22, 0xCC111111);

		int x = panelLeft + 8;
		int y = panelTop + 8;
		drawLine("WorldEdit", x, y, 0xFFFFFF);
		y += 20;
		y = drawSectionHeader("Selection", x, y);
		drawLine("Use Wand", x, y, 0xDDDDDD);
		y += 12;
		drawLine("Left click: Pos1", x, y, 0xDDDDDD);
		y += 12;
		drawLine("Right click: Pos2", x, y, 0xDDDDDD);
		y += 12;
		drawLine("Shown by particles", x, y, 0xAAAAAA);
		y += 14;
		y = drawSectionHeader("Clipboard", x, y);
		drawLine("Clipboard: " + (localClipboardCopied ? "Copied" : "Empty"), x, y,
				localClipboardCopied ? 0xA6FFAA : 0xCCCCCC);
		y += 12;
		drawLine("Limit: 64x64x64", x, y, 0xDDDDDD);
		y += 12;
		drawLine("= 262144", x, y, 0xDDDDDD);
		y += 14;
		y = drawSectionHeader("Preview", x, y);
		drawLine("Offset: " + getPreviewText(), x, y, previewSet ? 0xA6D8FF : 0xCCCCCC);
		y += 12;
		drawLine("Step: " + step, x, y, 0xFFFFAA);
		y += 14;
		y = drawSectionHeader("Job", x, y);
		drawLine("Job: " + localJobState, x, y, "Idle".equals(localJobState) ? 0xDDDDDD : 0xFFFFAA);
		y += 12;
		drawLine("Local UI state only", x, y, 0xAAAAAA);
		y += 12;
		drawLine("Wand selections may not sync", x, y, 0x888888);
	}

	private void drawLine(String text, int x, int y, int color) {
		this.drawString(this.fontRendererObj, text, x, y, color);
	}

	private int drawSectionHeader(String text, int x, int y) {
		drawLine(text, x, y, 0xFFE08A);
		return y + 12;
	}

	private int getPanelWidth() {
		return Math.min(182, Math.max(146, this.width / 2 - 12));
	}

	private String getPreviewText() {
		return previewSet ? previewOffsetX + " " + previewOffsetY + " " + previewOffsetZ : "None";
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}
