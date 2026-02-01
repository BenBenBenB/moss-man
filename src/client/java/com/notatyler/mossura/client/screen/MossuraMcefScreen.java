package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.ui.McefBrowserView;
import com.notatyler.mossura.client.ui.MossuraUiActionHandler;
import com.notatyler.mossura.client.ui.MossuraUiBridge;
import com.notatyler.mossura.client.ui.MossuraUiRoutes;
import com.notatyler.mossura.client.ui.MossuraUiRefreshable;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public abstract class MossuraMcefScreen extends Screen implements MossuraUiActionHandler, MossuraUiRefreshable {
	private static final int MAX_BROWSER_WIDTH = 560;
	private static final int MAX_BROWSER_HEIGHT = 360;
	private static final int MIN_BROWSER_WIDTH = 240;
	private static final int MIN_BROWSER_HEIGHT = 180;
	private static final int PADDING = 24;
	protected final McefBrowserView browserView;

	protected MossuraMcefScreen(Text title, String route) {
		super(title);
		this.browserView = new McefBrowserView(MossuraUiRoutes.url(route), true);
	}

	protected abstract JsonObject buildState();

	protected java.util.UUID getViewerId() {
		return client != null && client.player != null ? client.player.getUuid() : null;
	}

	protected String getViewerName() {
		return client != null && client.player != null ? client.player.getName().getString() : "Guest";
	}

	protected void pushState() {
		browserView.setState(buildState());
	}

	@Override
	public void refreshUiState() {
		pushState();
	}

	@Override
	protected void init() {
		super.init();
		if (client != null) {
			browserView.init(client, width, height);
			layoutBrowser();
			pushState();
			MossuraUiBridge.setActiveHandler(this);
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		layoutBrowser();
		pushState();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		drawFrame(context);
		if (browserView.isReady()) {
			browserView.render(context);
		} else {
			browserView.renderFallback(context, textRenderer);
		}
	}

	protected void layoutBrowser() {
		int availableWidth = Math.max(MIN_BROWSER_WIDTH, width - PADDING * 2);
		int availableHeight = Math.max(MIN_BROWSER_HEIGHT, height - PADDING * 2);
		int browserWidth = Math.min(MAX_BROWSER_WIDTH, availableWidth);
		int browserHeight = Math.min(MAX_BROWSER_HEIGHT, availableHeight);
		int x = (width - browserWidth) / 2;
		int y = (height - browserHeight) / 2;
		browserView.setBounds(x, y, browserWidth, browserHeight);
	}

	protected void drawFrame(DrawContext context) {
		int x = browserView.getDrawX();
		int y = browserView.getDrawY();
		int w = browserView.getDrawWidth();
		int h = browserView.getDrawHeight();
		context.fill(0, 0, width, height, 0x8A0B0D13);
		context.fill(x - 6, y - 6, x + w + 6, y + h + 6, 0xB0171A22);
	}

	@Override
	public void close() {
		MossuraUiBridge.clearActiveHandler(this);
		browserView.close();
		super.close();
	}

	@Override
	public void removed() {
		MossuraUiBridge.clearActiveHandler(this);
		super.removed();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubleClick) {
		if (browserView.handleMousePress(click.x(), click.y(), click.button())) {
			return true;
		}
		return super.mouseClicked(click, doubleClick);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (browserView.handleMouseRelease(click.x(), click.y(), click.button())) {
			return true;
		}
		return super.mouseReleased(click);
	}


	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		browserView.handleMouseMove(mouseX, mouseY);
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (browserView.handleMouseScroll(mouseX, mouseY, scrollY, 0)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		if (browserView.handleKeyPress(input.key(), input.scancode(), input.modifiers())) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		if (browserView.handleKeyRelease(input.key(), input.scancode(), input.modifiers())) {
			return true;
		}
		return super.keyReleased(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (browserView.handleCharTyped(input.codepoint(), input.modifiers())) {
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public boolean handleUiAction(String action, JsonObject payload) {
		return false;
	}
}
