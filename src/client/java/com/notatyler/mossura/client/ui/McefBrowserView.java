package com.notatyler.mossura.client.ui;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.listeners.MCEFInitListener;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class McefBrowserView {
	private static final Gson GSON = new Gson();

	private final String url;
	private final boolean transparent;
	private MCEFBrowser browser;
	private MinecraftClient client;
	private int drawX;
	private int drawY;
	private int drawWidth;
	private int drawHeight;
	private boolean initRequested;
	private boolean initFailed;
	private String pendingStateJson;
	private boolean needsInject;

	public McefBrowserView(String url, boolean transparent) {
		this.url = url;
		this.transparent = transparent;
	}

	public void init(MinecraftClient client, int width, int height) {
		this.client = client;
		updateBounds(0, 0, width, height);
		ensureBrowser();
		resizeBrowser();
	}

	public void updateBounds(int x, int y, int width, int height) {
		drawX = x;
		drawY = y;
		drawWidth = Math.max(1, width);
		drawHeight = Math.max(1, height);
	}

	public void resize(int width, int height) {
		updateBounds(drawX, drawY, width, height);
		resizeBrowser();
	}

	public void setBounds(int x, int y, int width, int height) {
		updateBounds(x, y, width, height);
		resizeBrowser();
	}

	public void close() {
		if (browser != null) {
			browser.close();
			browser = null;
		}
	}

	public boolean isReady() {
		return browser != null && browser.isTextureReady();
	}

	public boolean isInitFailed() {
		return initFailed;
	}

	public void setState(JsonElement state) {
		pendingStateJson = state == null ? "null" : GSON.toJson(state);
		needsInject = true;
	}

	public void render(DrawContext context) {
		if (browser == null) {
			return;
		}
		if (browser.isTextureReady()) {
			Identifier texture = browser.getTextureIdentifier();
			if (texture != null) {
				context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, 0.0F, 0.0F, drawWidth, drawHeight, drawWidth, drawHeight);
			}
			if (needsInject && pendingStateJson != null) {
				injectState();
			}
		}
	}

	public void renderFallback(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer) {
		context.fill(drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0xCC0C0F17);
		Text message = initFailed
				? Text.literal("MCEF failed to initialize. Check the logs and MCEF install.")
				: Text.literal("Loading Mossura UI...");
		context.drawText(textRenderer, message, drawX + 16, drawY + 16, 0xFFFFFFFF, false);
	}

	public boolean handleMousePress(double mouseX, double mouseY, int button) {
		if (browser == null || !isWithin(mouseX, mouseY)) {
			return false;
		}
		browser.sendMousePress(toBrowserX(mouseX), toBrowserY(mouseY), button);
		browser.setFocus(true);
		return true;
	}

	public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
		if (browser == null) {
			return false;
		}
		browser.sendMouseRelease(toBrowserX(mouseX), toBrowserY(mouseY), button);
		browser.setFocus(true);
		return true;
	}

	public void handleMouseMove(double mouseX, double mouseY) {
		if (browser == null || !isWithin(mouseX, mouseY)) {
			return;
		}
		browser.sendMouseMove(toBrowserX(mouseX), toBrowserY(mouseY));
	}

	public boolean handleMouseScroll(double mouseX, double mouseY, double amount, int modifiers) {
		if (browser == null || !isWithin(mouseX, mouseY)) {
			return false;
		}
		browser.sendMouseWheel(toBrowserX(mouseX), toBrowserY(mouseY), amount, modifiers);
		return true;
	}

	public boolean handleKeyPress(int key, long scancode, int modifiers) {
		if (browser == null) {
			return false;
		}
		browser.sendKeyPress(key, scancode, modifiers);
		browser.setFocus(true);
		return true;
	}

	public boolean handleKeyRelease(int key, long scancode, int modifiers) {
		if (browser == null) {
			return false;
		}
		browser.sendKeyRelease(key, scancode, modifiers);
		browser.setFocus(true);
		return true;
	}

	public boolean handleCharTyped(int codepoint, int modifiers) {
		if (browser == null) {
			return false;
		}
		if (codepoint == 0) {
			return false;
		}
		browser.sendKeyTyped((char) codepoint, modifiers);
		browser.setFocus(true);
		return true;
	}

	private void ensureBrowser() {
		if (initRequested || browser != null) {
			return;
		}
		initRequested = true;
		if (MCEF.isInitialized()) {
			createBrowser();
			return;
		}
		MCEF.scheduleForInit((MCEFInitListener) success -> {
			if (success) {
				createBrowser();
			} else {
				initFailed = true;
			}
		});
	}

	private void createBrowser() {
		if (browser != null) {
			return;
		}
		int scaledWidth = scaled(drawWidth);
		int scaledHeight = scaled(drawHeight);
		browser = MCEF.createBrowser(url, transparent, Math.max(1, scaledWidth), Math.max(1, scaledHeight));
	}

	private void resizeBrowser() {
		if (browser == null || client == null) {
			return;
		}
		browser.resize(Math.max(1, scaled(drawWidth)), Math.max(1, scaled(drawHeight)));
	}

	private int scaled(int value) {
		if (client == null) {
			return value;
		}
		return (int) Math.round(value * client.getWindow().getScaleFactor());
	}

	private int toBrowserX(double mouseX) {
		return scaled((int) Math.round(mouseX - drawX));
	}

	private int toBrowserY(double mouseY) {
		return scaled((int) Math.round(mouseY - drawY));
	}

	private boolean isWithin(double mouseX, double mouseY) {
		return mouseX >= drawX && mouseX < drawX + drawWidth && mouseY >= drawY && mouseY < drawY + drawHeight;
	}

	public int getDrawX() {
		return drawX;
	}

	public int getDrawY() {
		return drawY;
	}

	public int getDrawWidth() {
		return drawWidth;
	}

	public int getDrawHeight() {
		return drawHeight;
	}

	private void injectState() {
		String script = "window.__MOSSURA_STATE__ = " + pendingStateJson + ";" +
				"if (window.MossuraUI && window.MossuraUI.setState) { window.MossuraUI.setState(" + pendingStateJson + "); }";
		browser.executeJavaScript(script, url, 0);
		needsInject = false;
	}
}
