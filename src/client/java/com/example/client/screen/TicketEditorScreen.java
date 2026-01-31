package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.tickets.Ticket;
import com.example.tickets.TicketPriority;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TicketEditorScreen extends Screen {
	private final ProjectScreen parent;
	private final UUID projectId;
	private final UUID ticketId;
	private TextFieldWidget titleField;
	private TextFieldWidget descriptionField;
	private TextFieldWidget assigneeField;
	private CyclingButtonWidget<String> typeButton;
	private CyclingButtonWidget<String> stateButton;
	private CyclingButtonWidget<TicketPriority> priorityButton;
	private ButtonWidget saveButton;
	private ButtonWidget cancelButton;
	private ProjectData project;
	private Ticket ticket;

	private TicketEditorScreen(ProjectScreen parent, UUID projectId, UUID ticketId) {
		super(Text.literal(ticketId == null ? "Create Ticket" : "Edit Ticket"));
		this.parent = parent;
		this.projectId = projectId;
		this.ticketId = ticketId;
	}

	public static TicketEditorScreen createNew(ProjectScreen parent, UUID projectId) {
		return new TicketEditorScreen(parent, projectId, null);
	}

	public static TicketEditorScreen editExisting(ProjectScreen parent, UUID ticketId) {
		return new TicketEditorScreen(parent, parent.getProjectId(), ticketId);
	}

	@Override
	protected void init() {
		super.init();
		project = ClientProjectCache.get(projectId);
		if (ticketId != null && project != null) {
			ticket = project.findTicket(ticketId);
		}
		int left = this.width / 2 - 120;
		int top = 40;
		titleField = new TextFieldWidget(textRenderer, left, top, 220, 16, Text.literal("Title"));
		descriptionField = new TextFieldWidget(textRenderer, left, top + 24, 220, 16, Text.literal("Description"));
		assigneeField = new TextFieldWidget(textRenderer, left, top + 48, 220, 16, Text.literal("Assignee (name)"));

		List<String> types = project == null ? List.of("Task") : project.getTicketTypes();
		List<String> states = project == null ? List.of("To Do") : project.getStatuses();

		String initialType = ticket != null ? ticket.getType() : types.get(0);
		String initialState = ticket != null ? ticket.getState() : states.get(0);
		TicketPriority initialPriority = ticket != null ? ticket.getPriority() : TicketPriority.MEDIUM;

		typeButton = CyclingButtonWidget.builder(Text::literal, initialType)
				.values(types)
				.build(left, top + 72, 100, 16, Text.literal("Type"));
		stateButton = CyclingButtonWidget.builder(Text::literal, initialState)
				.values(states)
				.build(left + 110, top + 72, 110, 16, Text.literal("State"));
		priorityButton = CyclingButtonWidget.builder(priority -> Text.literal(priority.name()), initialPriority)
				.values(TicketPriority.values())
				.build(left, top + 96, 120, 16, Text.literal("Priority"));

		saveButton = ButtonWidget.builder(Text.literal("Save"), button -> saveTicket())
				.position(left, top + 128)
				.size(80, 18)
				.build();
		cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> returnToParent())
				.position(left + 90, top + 128)
				.size(80, 18)
				.build();

		addDrawableChild(titleField);
		addDrawableChild(descriptionField);
		addDrawableChild(assigneeField);
		addDrawableChild(typeButton);
		addDrawableChild(stateButton);
		addDrawableChild(priorityButton);
		addDrawableChild(saveButton);
		addDrawableChild(cancelButton);

		if (ticket != null) {
			titleField.setText(ticket.getTitle());
			descriptionField.setText(ticket.getDescription());
			if (ticket.getAssigneeName() != null) {
				assigneeField.setText(ticket.getAssigneeName());
			}
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		context.drawText(textRenderer, Text.literal(ticketId == null ? "Create Ticket" : "Edit Ticket"), this.width / 2 - 40, 18, 0xFFFFFF, false);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xC0101010);
	}

	private void saveTicket() {
		String title = titleField.getText();
		String description = descriptionField.getText();
		String type = typeButton.getValue();
		String state = stateButton.getValue();
		TicketPriority priority = priorityButton.getValue();
		String assigneeName = assigneeField.getText().isBlank() ? null : assigneeField.getText();

		if (ticketId == null) {
			ClientPlayNetworking.send(new MossuraPayloads.CreateTicketPayload(projectId, title, description, type, state, priority, assigneeName));
		} else {
			ClientPlayNetworking.send(new MossuraPayloads.UpdateTicketPayload(projectId, ticketId, title, description, type, state, priority, assigneeName));
		}
		returnToParent();
	}

	private void returnToParent() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(parent);
		}
	}
}
