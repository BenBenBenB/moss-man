package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.screen.ProjectScreenHandler;
import com.example.tickets.Ticket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;

public class ProjectScreen extends HandledScreen<ProjectScreenHandler> {
	private ProjectData project;
	private boolean boardView;
	private boolean switching;
	private final List<TicketHitbox> ticketHitboxes = new ArrayList<>();
	private ButtonWidget listButton;
	private ButtonWidget boardButton;
	private ButtonWidget settingsButton;
	private ButtonWidget newTicketButton;
	private TextFieldWidget searchField;
	private CyclingButtonWidget<String> statusFilterButton;

	public ProjectScreen(ProjectScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 320;
		this.backgroundHeight = 240;
	}

	public UUID getProjectId() {
		return handler.getProjectId();
	}

	public void applyProjectSync(ProjectData project) {
		this.project = project;
		ClientProjectCache.put(project);
	}

	@Override
	protected void init() {
		this.backgroundWidth = Math.min(360, this.width - 30);
		this.backgroundHeight = Math.min(260, this.height - 30);
		super.init();
		if (project == null) {
			project = ProjectData.fromNbt(handler.getInitialProjectNbt());
			ClientProjectCache.put(project);
		}
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		int x = this.x + 8;
		int y = this.y + 6;
		int right = this.x + this.backgroundWidth;
		listButton = ButtonWidget.builder(Text.literal("List"), button -> boardView = false)
				.position(x, y)
				.size(40, 16)
				.build();
		boardButton = ButtonWidget.builder(Text.literal("Board"), button -> boardView = true)
				.position(x + 44, y)
				.size(44, 16)
				.build();
		settingsButton = ButtonWidget.builder(Text.literal("Settings"), button -> openChild(new ProjectSettingsScreen(this)))
				.position(right - 80, y)
				.size(54, 16)
				.build();
		newTicketButton = ButtonWidget.builder(Text.literal("+"), button -> openChild(TicketEditorScreen.createNew(this, handler.getProjectId())))
				.position(right - 20, y)
				.size(16, 16)
				.build();
		searchField = new TextFieldWidget(textRenderer, x, y + 56, 140, 16, Text.literal("Search"));
		styleField(searchField);
		List<String> filterValues = new java.util.ArrayList<>();
		filterValues.add("All");
		if (project != null) {
			filterValues.addAll(project.getStatuses());
		}
		statusFilterButton = CyclingButtonWidget.builder(Text::literal, filterValues.get(0))
				.values(filterValues)
				.build(x + 150, y + 56, 90, 16, Text.literal("Status"));
		addDrawableChild(listButton);
		addDrawableChild(boardButton);
		addDrawableChild(settingsButton);
		addDrawableChild(newTicketButton);
		addDrawableChild(searchField);
		addDrawableChild(statusFilterButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ProjectData latest = ClientProjectCache.get(handler.getProjectId());
		if (latest != null) {
			project = latest;
		}
		super.render(context, mouseX, mouseY, delta);
		if (project != null) {
			int scissorWidth = client == null ? this.width : client.getWindow().getScaledWidth();
			int scissorHeight = client == null ? this.height : client.getWindow().getScaledHeight();
			context.enableScissor(0, 0, scissorWidth, scissorHeight);
		context.drawText(this.textRenderer, Text.literal(project.getName()), this.x + 8, this.y + 26, 0xFFFFFFFF, false);
		context.drawText(this.textRenderer, Text.literal("Owner: " + project.getOwnerName()), this.x + 8, this.y + 40, 0xFFE0E0E0, false);
			ticketHitboxes.clear();
			if (boardView) {
				drawBoardView(context);
			} else {
				drawListView(context);
			}
			context.disableScissor();
		}
		drawPlaceholder(context, searchField, "Search");
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		// Hide default container and inventory titles to prevent overlap.
	}

	private void drawListView(DrawContext context) {
		int startY = this.y + 86;
		context.drawText(this.textRenderer, Text.literal("Tickets"), this.x + 8, startY - 12, 0xFFE0E0E0, false);
		int rowHeight = 12;
		int maxRows = (this.backgroundHeight - 100) / rowHeight;
		List<Ticket> tickets = project.getTickets();
		String filterText = searchField == null ? "" : searchField.getText().toLowerCase();
		String statusFilter = statusFilterButton == null ? "All" : statusFilterButton.getValue();
		int row = 0;
		for (int i = 0; i < tickets.size() && i < maxRows; i++) {
			Ticket ticket = tickets.get(i);
			if (ticket.isDeleted()) {
				continue;
			}
			if (!"All".equals(statusFilter) && !ticket.getState().equals(statusFilter)) {
				continue;
			}
			if (!filterText.isBlank()) {
				String haystack = (ticket.getTitle() + " " + ticket.getDescription() + " " + ticket.getNumber()).toLowerCase();
				if (!haystack.contains(filterText)) {
					continue;
				}
			}
			int y = startY + row * rowHeight;
			String label = "#" + ticket.getNumber() + " " + ticket.getTitle() + " [" + ticket.getState() + "]";
			context.drawText(this.textRenderer, Text.literal(label), this.x + 8, y, 0xFFFFFFFF, false);
			ticketHitboxes.add(new TicketHitbox(ticket.getId(), this.x + 6, y - 1, this.backgroundWidth - 12, rowHeight));
			row++;
			if (row >= maxRows) {
				break;
			}
		}
	}

	private void drawBoardView(DrawContext context) {
		List<String> statuses = project.getStatuses();
		int columnCount = Math.max(1, statuses.size());
		int columnWidth = (this.backgroundWidth - 16) / columnCount;
		int top = this.y + 86;
		for (int i = 0; i < statuses.size(); i++) {
			int x = this.x + 8 + i * columnWidth;
			context.drawText(this.textRenderer, Text.literal(statuses.get(i)), x + 2, top - 12, 0xFFE0E0E0, false);
			int row = 0;
			for (Ticket ticket : project.getTickets()) {
				if (ticket.isDeleted()) {
					continue;
				}
				if (!ticket.getState().equals(statuses.get(i))) {
					continue;
				}
				int y = top + row * 16;
				String label = "#" + ticket.getNumber() + " " + ticket.getTitle();
				context.drawText(this.textRenderer, Text.literal(label), x + 2, y, 0xFFFFFFFF, false);
				ticketHitboxes.add(new TicketHitbox(ticket.getId(), x, y - 1, columnWidth - 4, 14));
				row++;
				if (y > this.y + this.backgroundHeight - 24) {
					break;
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubleClick) {
		double mouseX = click.x();
		double mouseY = click.y();
		for (TicketHitbox hitbox : ticketHitboxes) {
			if (hitbox.contains(mouseX, mouseY)) {
				openChild(new TicketViewerScreen(this, hitbox.ticketId));
				return true;
			}
		}
		return super.mouseClicked(click, doubleClick);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF1E1E1E);
	}

	public ProjectData getProject() {
		return project;
	}

	public void openChild(Screen child) {
		switching = true;
		if (client != null) {
			client.setScreen(child);
		}
		switching = false;
	}

	@Override
	public void removed() {
		if (!switching) {
			super.removed();
		}
	}

	private static class TicketHitbox {
		private final UUID ticketId;
		private final double x;
		private final double y;
		private final double width;
		private final double height;

		private TicketHitbox(UUID ticketId, double x, double y, double width, double height) {
			this.ticketId = ticketId;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
		}
	}

	private static void styleField(TextFieldWidget field) {
		field.setEditableColor(0xFFFFFFFF);
		field.setUneditableColor(0xFFB0B0B0);
		field.setDrawsBackground(true);
	}

	private void drawPlaceholder(DrawContext context, TextFieldWidget field, String placeholder) {
		if (field == null || field.isFocused() || !field.getText().isEmpty()) {
			return;
		}
		int x = field.getX() + 4;
		int y = field.getY() + (field.getHeight() - textRenderer.fontHeight) / 2 + 1;
		context.drawText(textRenderer, Text.literal(placeholder), x, y, 0xFF7A7A7A, false);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (searchField != null && searchField.isFocused()) {
			if (searchField.keyPressed(input)) {
				return true;
			}
			if (client != null && client.options.inventoryKey.matchesKey(input)) {
				return true;
			}
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (searchField != null && searchField.isFocused() && searchField.charTyped(input)) {
			return true;
		}
		return super.charTyped(input);
	}
}
