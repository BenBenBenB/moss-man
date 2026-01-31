package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;

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
		this.backgroundWidth = 248;
		this.backgroundHeight = 210;
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
		super.init();
		if (project == null) {
			project = ProjectData.fromNbt(handler.getInitialProjectNbt());
			ClientProjectCache.put(project);
		}
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
		searchField = new TextFieldWidget(textRenderer, x, y + 20, 120, 16, Text.literal("Search"));
		List<String> filterValues = new java.util.ArrayList<>();
		filterValues.add("All");
		if (project != null) {
			filterValues.addAll(project.getStatuses());
		}
		statusFilterButton = CyclingButtonWidget.builder(Text::literal, filterValues.get(0))
				.values(filterValues)
				.build(x + 126, y + 20, 90, 16, Text.literal("Status"));
		addDrawableChild(listButton);
		addDrawableChild(boardButton);
		addDrawableChild(settingsButton);
		addDrawableChild(newTicketButton);
		addDrawableChild(searchField);
		addDrawableChild(statusFilterButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		if (project != null) {
			context.drawText(this.textRenderer, Text.literal(project.getName()), this.x + 8, this.y + 28, 0xFFFFFF, false);
			context.drawText(this.textRenderer, Text.literal("Owner: " + project.getOwnerName()), this.x + 8, this.y + 40, 0xAAAAAA, false);
			ticketHitboxes.clear();
			if (boardView) {
				drawBoardView(context);
			} else {
				drawListView(context);
			}
		}
		super.render(context, mouseX, mouseY, delta);
	}

	private void drawListView(DrawContext context) {
		int startY = this.y + 58;
		context.drawText(this.textRenderer, Text.literal("Tickets"), this.x + 8, startY - 12, 0xE0E0E0, false);
		int rowHeight = 12;
		int maxRows = (this.backgroundHeight - 70) / rowHeight;
		List<Ticket> tickets = project.getTickets();
		String filterText = searchField == null ? "" : searchField.getText().toLowerCase();
		String statusFilter = statusFilterButton == null ? "All" : statusFilterButton.getValue();
		int row = 0;
		for (int i = 0; i < tickets.size() && i < maxRows; i++) {
			Ticket ticket = tickets.get(i);
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
			context.drawText(this.textRenderer, Text.literal(label), this.x + 8, y, 0xFFFFFF, false);
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
		int top = this.y + 58;
		for (int i = 0; i < statuses.size(); i++) {
			int x = this.x + 8 + i * columnWidth;
			context.drawText(this.textRenderer, Text.literal(statuses.get(i)), x + 2, top - 12, 0xE0E0E0, false);
			int row = 0;
			for (Ticket ticket : project.getTickets()) {
				if (!ticket.getState().equals(statuses.get(i))) {
					continue;
				}
				int y = top + row * 16;
				String label = "#" + ticket.getNumber() + " " + ticket.getTitle();
				context.drawText(this.textRenderer, Text.literal(label), x + 2, y, 0xFFFFFF, false);
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
}
