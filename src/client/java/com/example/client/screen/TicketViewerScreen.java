package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.tickets.Comment;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;

public class TicketViewerScreen extends Screen {
	private final ProjectScreen parent;
	private final UUID ticketId;
	private Ticket ticket;
	private TextFieldWidget commentField;
	private TextFieldWidget subtaskNameField;
	private TextFieldWidget subtaskDescField;
	private TextFieldWidget subtaskCommentField;
	private ButtonWidget addCommentButton;
	private ButtonWidget addSubtaskButton;
	private ButtonWidget addSubtaskCommentButton;
	private ButtonWidget editButton;
	private ButtonWidget backButton;
	private UUID selectedSubtaskId;

	public TicketViewerScreen(ProjectScreen parent, UUID ticketId) {
		super(Text.literal("Ticket Viewer"));
		this.parent = parent;
		this.ticketId = ticketId;
	}

	@Override
	protected void init() {
		super.init();
		refreshTicket();
		int left = this.width / 2 - 120;
		int top = 24;
		backButton = ButtonWidget.builder(Text.literal("Back"), button -> returnToParent())
				.position(left, top)
				.size(50, 16)
				.build();
		editButton = ButtonWidget.builder(Text.literal("Edit"), button -> openEditor())
				.position(left + 56, top)
				.size(50, 16)
				.build();
		addDrawableChild(backButton);
		addDrawableChild(editButton);

		commentField = new TextFieldWidget(textRenderer, left, top + 160, 180, 16, Text.literal("Comment"));
		addDrawableChild(commentField);
		addCommentButton = ButtonWidget.builder(Text.literal("Add Comment"), button -> sendComment())
				.position(left + 186, top + 160)
				.size(90, 16)
				.build();
		addDrawableChild(addCommentButton);

		subtaskNameField = new TextFieldWidget(textRenderer, left, top + 86, 120, 16, Text.literal("Subtask name"));
		subtaskDescField = new TextFieldWidget(textRenderer, left + 124, top + 86, 150, 16, Text.literal("Subtask description"));
		addDrawableChild(subtaskNameField);
		addDrawableChild(subtaskDescField);
		addSubtaskButton = ButtonWidget.builder(Text.literal("Add Subtask"), button -> sendSubtask())
				.position(left + 278, top + 86)
				.size(80, 16)
				.build();
		addDrawableChild(addSubtaskButton);

		subtaskCommentField = new TextFieldWidget(textRenderer, left, top + 130, 180, 16, Text.literal("Subtask comment"));
		addDrawableChild(subtaskCommentField);
		addSubtaskCommentButton = ButtonWidget.builder(Text.literal("Add Subtask Comment"), button -> sendSubtaskComment())
				.position(left + 186, top + 130)
				.size(120, 16)
				.build();
		addDrawableChild(addSubtaskCommentButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		refreshTicket();
		int left = this.width / 2 - 120;
		int y = 48;
		if (ticket != null) {
			context.drawText(textRenderer, Text.literal("#" + ticket.getNumber() + " " + ticket.getTitle()), left, y, 0xFFFFFF, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Type: " + ticket.getType() + " | State: " + ticket.getState()), left, y, 0xCCCCCC, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Priority: " + ticket.getPriority()), left, y, 0xCCCCCC, false);
			y += 12;
			String assignee = ticket.getAssigneeName() == null ? "Unassigned" : ticket.getAssigneeName();
			context.drawText(textRenderer, Text.literal("Assignee: " + assignee), left, y, 0xCCCCCC, false);
			y += 16;
			context.drawText(textRenderer, Text.literal(ticket.getDescription()), left, y, 0xAAAAAA, false);
			y += 20;
			context.drawText(textRenderer, Text.literal("Subtasks:"), left, y, 0xE0E0E0, false);
			y += 12;
			for (Subtask subtask : ticket.getSubtasks()) {
				int color = subtask.getId().equals(selectedSubtaskId) ? 0x00FFAA : 0xFFFFFF;
				context.drawText(textRenderer, Text.literal("- " + subtask.getName() + " (" + subtask.getCreatorName() + ")"), left + 4, y, color, false);
				y += 10;
				if (y > 120) {
					break;
				}
			}
			y = 120;
			context.drawText(textRenderer, Text.literal("Comments:"), left, y, 0xE0E0E0, false);
			y += 12;
			for (Comment comment : ticket.getComments()) {
				context.drawText(textRenderer, Text.literal(comment.getAuthorName() + ": " + comment.getMessage()), left + 4, y, 0xFFFFFF, false);
				y += 10;
				if (y > 150) {
					break;
				}
			}
		}
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xC0101010);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubleClick) {
		double mouseX = click.x();
		double mouseY = click.y();
		if (ticket != null) {
			int left = this.width / 2 - 120;
			int y = 72;
			for (Subtask subtask : ticket.getSubtasks()) {
				if (mouseX >= left && mouseX <= left + 200 && mouseY >= y && mouseY <= y + 10) {
					selectedSubtaskId = subtask.getId();
					return true;
				}
				y += 10;
				if (y > 120) {
					break;
				}
			}
		}
		return super.mouseClicked(click, doubleClick);
	}

	private void sendComment() {
		if (ticket == null || commentField.getText().isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddTicketCommentPayload(parent.getProjectId(), ticket.getId(), commentField.getText()));
		commentField.setText("");
	}

	private void sendSubtask() {
		if (ticket == null || subtaskNameField.getText().isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskPayload(parent.getProjectId(), ticket.getId(), subtaskNameField.getText(), subtaskDescField.getText()));
		subtaskNameField.setText("");
		subtaskDescField.setText("");
	}

	private void sendSubtaskComment() {
		if (ticket == null || selectedSubtaskId == null || subtaskCommentField.getText().isBlank()) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskCommentPayload(parent.getProjectId(), ticket.getId(), selectedSubtaskId, subtaskCommentField.getText()));
		subtaskCommentField.setText("");
	}

	private void openEditor() {
		if (ticket == null) {
			return;
		}
		parent.openChild(TicketEditorScreen.editExisting(parent, ticket.getId()));
	}

	private void returnToParent() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	private void refreshTicket() {
		ProjectData latest = ClientProjectCache.get(parent.getProjectId());
		if (latest != null) {
			this.ticket = latest.findTicket(ticketId);
		}
	}
}
