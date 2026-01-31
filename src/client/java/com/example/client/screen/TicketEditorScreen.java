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
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import org.lwjgl.glfw.GLFW;

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

	private Layout layout() {
		int left = this.width / 2 - 120;
		int startY = 30;
		int labelGap = 10;
		int rowGap = 30;
		int titleLabelY = startY;
		int titleFieldY = titleLabelY + labelGap;
		int descLabelY = titleLabelY + rowGap;
		int descFieldY = descLabelY + labelGap;
		int assigneeLabelY = descLabelY + rowGap;
		int assigneeFieldY = assigneeLabelY + labelGap;
		int typeLabelY = assigneeLabelY + rowGap;
		int typeRowY = typeLabelY + labelGap;
		int priorityLabelY = typeLabelY + rowGap;
		int priorityRowY = priorityLabelY + labelGap;
		int buttonsY = priorityRowY + 26;
		return new Layout(left, titleLabelY, titleFieldY, descLabelY, descFieldY, assigneeLabelY, assigneeFieldY, typeLabelY, typeRowY, priorityLabelY, priorityRowY, buttonsY);
	}

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
		Layout layout = layout();
		titleField = new TextFieldWidget(textRenderer, layout.left, layout.titleFieldY, 220, 16, Text.literal("Title"));
		descriptionField = new TextFieldWidget(textRenderer, layout.left, layout.descFieldY, 220, 16, Text.literal("Description"));
		assigneeField = new TextFieldWidget(textRenderer, layout.left, layout.assigneeFieldY, 220, 16, Text.literal("Assignee (name)"));
		styleField(titleField);
		styleField(descriptionField);
		styleField(assigneeField);

		List<String> types = project == null ? List.of("Task") : project.getTicketTypes();
		List<String> states = project == null ? List.of("To Do") : project.getStatuses();

		String initialType = ticket != null ? ticket.getType() : types.get(0);
		String initialState = ticket != null ? ticket.getState() : states.get(0);
		TicketPriority initialPriority = ticket != null ? ticket.getPriority() : TicketPriority.MEDIUM;

		typeButton = CyclingButtonWidget.builder(Text::literal, initialType)
				.values(types)
				.build(layout.left, layout.typeRowY, 100, 16, Text.literal("Type"));
		stateButton = CyclingButtonWidget.builder(Text::literal, initialState)
				.values(states)
				.build(layout.left + 110, layout.typeRowY, 110, 16, Text.literal("State"));
		priorityButton = CyclingButtonWidget.builder(priority -> Text.literal(priority.name()), initialPriority)
				.values(TicketPriority.values())
				.build(layout.left, layout.priorityRowY, 120, 16, Text.literal("Priority"));

		saveButton = ButtonWidget.builder(Text.literal("Save"), button -> saveTicket())
				.position(layout.left, layout.buttonsY)
				.size(80, 18)
				.build();
		cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> returnToParent())
				.position(layout.left + 90, layout.buttonsY)
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
		Layout layout = layout();
		context.drawText(textRenderer, Text.literal(ticketId == null ? "Create Ticket" : "Edit Ticket"), this.width / 2 - 40, 18, 0xFFFFFFFF, false);
		context.drawText(textRenderer, Text.literal("Title"), layout.left, layout.titleLabelY, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Description"), layout.left, layout.descLabelY, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Assignee"), layout.left, layout.assigneeLabelY, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Type"), layout.left, layout.typeLabelY, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("State"), layout.left + 110, layout.typeLabelY, 0xFFE6E6E6, false);
		context.drawText(textRenderer, Text.literal("Priority"), layout.left, layout.priorityLabelY, 0xFFE6E6E6, false);
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
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(projectId));
		returnToParent();
	}

	private void returnToParent() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	private static void styleField(TextFieldWidget field) {
		field.setEditableColor(0xFFFFFFFF);
		field.setUneditableColor(0xFFB0B0B0);
		field.setDrawsBackground(true);
	}

	private record Layout(int left, int titleLabelY, int titleFieldY, int descLabelY, int descFieldY,
						  int assigneeLabelY, int assigneeFieldY, int typeLabelY, int typeRowY,
						  int priorityLabelY, int priorityRowY, int buttonsY) {
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			saveTicket();
			return true;
		}
		if (handleFieldKeyPress(titleField, input)) {
			return true;
		}
		if (handleFieldKeyPress(descriptionField, input)) {
			return true;
		}
		if (handleFieldKeyPress(assigneeField, input)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (handleFieldCharTyped(titleField, input)) {
			return true;
		}
		if (handleFieldCharTyped(descriptionField, input)) {
			return true;
		}
		if (handleFieldCharTyped(assigneeField, input)) {
			return true;
		}
		return super.charTyped(input);
	}

	private boolean handleFieldKeyPress(TextFieldWidget field, KeyInput input) {
		if (field == null || !field.isFocused()) {
			return false;
		}
		if (field.keyPressed(input)) {
			return true;
		}
		if (client != null && client.options.inventoryKey.matchesKey(input)) {
			return true;
		}
		return false;
	}

	private boolean handleFieldCharTyped(TextFieldWidget field, CharInput input) {
		return field != null && field.isFocused() && field.charTyped(input);
	}
}
