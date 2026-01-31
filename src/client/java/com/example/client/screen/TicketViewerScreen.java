package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.tickets.Comment;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import org.lwjgl.glfw.GLFW;

public class TicketViewerScreen extends Screen {
	private final ProjectScreen parent;
	private final UUID ticketId;
	private Ticket ticket;
	private ButtonWidget editButton;
	private ButtonWidget backButton;
	private ButtonWidget deleteButton;
	private ButtonWidget addCommentButton;
	private ButtonWidget addSubtaskButton;
	private final List<SubtaskHitbox> subtaskHitboxes = new ArrayList<>();
	private static final int LINE_HEIGHT = 10;

	public TicketViewerScreen(ProjectScreen parent, UUID ticketId) {
		super(Text.literal("Ticket Viewer"));
		this.parent = parent;
		this.ticketId = ticketId;
	}

	@Override
	protected void init() {
		super.init();
		refreshTicket();
		Layout layout = layout();
		int left = layout.left;
		int top = 20;
		backButton = ButtonWidget.builder(Text.literal("Back"), button -> returnToParent())
				.position(left, top)
				.size(50, 16)
				.build();
		editButton = ButtonWidget.builder(Text.literal("Edit"), button -> openEditor())
				.position(left + 56, top)
				.size(50, 16)
				.build();
		deleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> deleteTicket())
				.position(left + 112, top)
				.size(60, 16)
				.build();
		addCommentButton = ButtonWidget.builder(Text.literal("Add Comment"), button -> openAddComment())
				.position(left, top + 20)
				.size(90, 16)
				.build();
		addSubtaskButton = ButtonWidget.builder(Text.literal("Add Subtask"), button -> openAddSubtask())
				.position(left + 96, top + 20)
				.size(90, 16)
				.build();
		addDrawableChild(backButton);
		addDrawableChild(editButton);
		addDrawableChild(deleteButton);
		addDrawableChild(addCommentButton);
		addDrawableChild(addSubtaskButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		refreshTicket();
		Layout layout = layout();
		int left = layout.left;
		int y = layout.contentStartY;
		subtaskHitboxes.clear();
		if (ticket != null) {
			context.drawText(textRenderer, Text.literal("#" + ticket.getNumber() + " " + ticket.getTitle()), left, y, 0xFFFFFFFF, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Type: " + ticket.getType() + " | State: " + ticket.getState()), left, y, 0xFFCCCCCC, false);
			y += 12;
			context.drawText(textRenderer, Text.literal("Priority: " + ticket.getPriority()), left, y, 0xFFCCCCCC, false);
			y += 12;
			String assignee = ticket.getAssigneeName() == null ? "Unassigned" : ticket.getAssigneeName();
			context.drawText(textRenderer, Text.literal("Assignee: " + assignee), left, y, 0xFFCCCCCC, false);
			y += 14;
			context.drawText(textRenderer, Text.literal("Description:"), left, y, 0xFFE0E0E0, false);
			y += 12;
			for (OrderedText line : textRenderer.wrapLines(Text.literal(ticket.getDescription()), layout.contentWidth)) {
				context.drawText(textRenderer, line, left, y, 0xFFAAAAAA, false);
				y += LINE_HEIGHT;
				if (y > this.height - 60) {
					break;
				}
			}
			y += 6;
			context.drawText(textRenderer, Text.literal("Subtasks:"), left, y, 0xFFE0E0E0, false);
			y += 12;
			for (Subtask subtask : ticket.getSubtasks()) {
				boolean completed = subtask.isCompleted();
				String prefix = completed ? "[x] " : "[ ] ";
				int color = completed ? 0xFF9AA0A6 : 0xFFFFFFFF;
				context.drawText(textRenderer, Text.literal(prefix + subtask.getName() + " (" + subtask.getCreatorName() + ")"), left + 4, y, color, false);
				subtaskHitboxes.add(new SubtaskHitbox(subtask.getId(), left, y, layout.contentWidth, LINE_HEIGHT));
				y += LINE_HEIGHT;
				if (!subtask.getDescription().isBlank()) {
					for (OrderedText line : textRenderer.wrapLines(Text.literal(subtask.getDescription()), layout.contentWidth - 12)) {
						context.drawText(textRenderer, line, left + 12, y, 0xFFAAAAAA, false);
						y += LINE_HEIGHT;
						if (y > this.height - 60) {
							break;
						}
					}
				}
				if (!subtask.getComments().isEmpty()) {
					for (Comment comment : subtask.getComments()) {
						Text lineText = Text.literal(comment.getAuthorName() + ": " + comment.getMessage());
						for (OrderedText line : textRenderer.wrapLines(lineText, layout.contentWidth - 12)) {
							context.drawText(textRenderer, line, left + 12, y, 0xFFDDDDDD, false);
							y += LINE_HEIGHT;
							if (y > this.height - 60) {
								break;
							}
						}
						if (y > this.height - 60) {
							break;
						}
					}
				}
				y += 4;
				if (y > this.height - 60) {
					break;
				}
			}
			y += 6;
			context.drawText(textRenderer, Text.literal("Comments:"), left, y, 0xFFE0E0E0, false);
			y += 12;
			for (Comment comment : ticket.getComments()) {
				Text lineText = Text.literal(comment.getAuthorName() + ": " + comment.getMessage());
				for (OrderedText line : textRenderer.wrapLines(lineText, layout.contentWidth)) {
					context.drawText(textRenderer, line, left + 4, y, 0xFFFFFFFF, false);
					y += LINE_HEIGHT;
					if (y > this.height - 30) {
						break;
					}
				}
				if (y > this.height - 30) {
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
		if (!subtaskHitboxes.isEmpty()) {
			for (SubtaskHitbox hitbox : subtaskHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
						openAddSubtaskComment(hitbox.subtaskId);
						return true;
					}
					if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
						if (doubleClick) {
							openEditSubtask(hitbox.subtaskId);
						} else {
							toggleSubtaskComplete(hitbox.subtaskId);
						}
						return true;
					}
				}
			}
		}
		return super.mouseClicked(click, doubleClick);
	}

	private void deleteTicket() {
		if (ticket == null) {
			return;
		}
		ClientPlayNetworking.send(new MossuraPayloads.DeleteTicketPayload(parent.getProjectId(), ticket.getId()));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(parent.getProjectId()));
		returnToParent();
	}

	private void openAddComment() {
		if (ticket == null) {
			return;
		}
		openChild(new TicketCommentScreen(this, parent.getProjectId(), ticket.getId()));
	}

	private void openAddSubtask() {
		if (ticket == null) {
			return;
		}
		openChild(new SubtaskCreateScreen(this, parent.getProjectId(), ticket.getId()));
	}

	private void openAddSubtaskComment(UUID subtaskId) {
		if (ticket == null || subtaskId == null) {
			return;
		}
		openChild(new SubtaskCommentScreen(this, parent.getProjectId(), ticket.getId(), subtaskId));
	}

	private void openEditSubtask(UUID subtaskId) {
		if (ticket == null || subtaskId == null) {
			return;
		}
		Subtask subtask = ticket.findSubtask(subtaskId);
		if (subtask == null) {
			return;
		}
		SubtaskEditScreen screen = new SubtaskEditScreen(this, parent.getProjectId(), ticket.getId(), subtask);
		openChild(screen);
		screen.setInitialValues(subtask.getName(), subtask.getDescription());
	}

	private void toggleSubtaskComplete(UUID subtaskId) {
		if (ticket == null || subtaskId == null) {
			return;
		}
		Subtask subtask = ticket.findSubtask(subtaskId);
		if (subtask == null) {
			return;
		}
		boolean newState = !subtask.isCompleted();
		ClientPlayNetworking.send(new MossuraPayloads.SetSubtaskCompletePayload(parent.getProjectId(), ticket.getId(), subtask.getId(), newState));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(parent.getProjectId()));
	}

	private void openChild(Screen child) {
		if (client != null) {
			client.setScreen(child);
		}
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

	private Layout layout() {
		int contentWidth = Math.min(340, this.width - 40);
		int left = (this.width - contentWidth) / 2;
		int contentStartY = 92;
		return new Layout(left, contentWidth, contentStartY);
	}

	private int subtaskStartY(Layout layout, Ticket ticket) {
		int y = layout.contentStartY;
		y += 12; // title
		y += 12; // type/state
		y += 12; // priority
		y += 14; // assignee
		y += 12; // description label
		y += textRenderer.wrapLines(Text.literal(ticket.getDescription()), layout.contentWidth).size() * LINE_HEIGHT;
		y += 6; // gap
		y += 12; // subtasks label
		return y;
	}

	private record Layout(int left, int contentWidth, int contentStartY) {
	}

	private static class SubtaskHitbox {
		private final UUID subtaskId;
		private final double x;
		private final double y;
		private final double width;
		private final double height;

		private SubtaskHitbox(UUID subtaskId, double x, double y, double width, double height) {
			this.subtaskId = subtaskId;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			returnToParent();
			return true;
		}
		return super.keyPressed(input);
	}
}
