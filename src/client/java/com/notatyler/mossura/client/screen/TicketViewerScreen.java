package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.network.MossuraPayloads;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.tickets.Subtask;
import com.notatyler.mossura.tickets.Ticket;
import com.notatyler.mossura.tickets.TicketPriority;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

public class TicketViewerScreen extends MossuraMcefScreen {
	private final ProjectScreen parent;
	private final UUID ticketId;

	public TicketViewerScreen(ProjectScreen parent, UUID ticketId) {
		super(Text.literal("Ticket"), "ticket/" + ticketId);
		this.parent = parent;
		this.ticketId = ticketId;
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return MossuraUiState.loadingState("ticket");
		}
		Ticket ticket = project.findTicket(ticketId);
		net.minecraft.client.network.ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
		UUID pid = player == null ? null : player.getUuid();
		String pName = player == null ? null : player.getName().getString();
		if (ticket == null) {
			return MossuraUiState.screenState("ticket", project, pid, pName);
		}
		return MossuraUiState.ticketState(project, ticket, pid, pName);
	}

	@Override
	public boolean handleUiAction(String action, JsonObject payload) {
		if (payload == null) {
			payload = new JsonObject();
		}
		return switch (action) {
			case "update-ticket" -> handleUpdateTicket(payload);
			case "add-ticket-comment" -> handleTicketComment(payload);
			case "add-subtask" -> handleAddSubtask(payload);
			case "update-subtask" -> handleUpdateSubtask(payload);
			case "add-subtask-comment" -> handleSubtaskComment(payload);
			case "toggle-subtask" -> handleToggleSubtask(payload);
			default -> false;
		};
	}

	private boolean handleUpdateTicket(JsonObject payload) {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return false;
		}
		UUID targetId = readUuid(payload, "ticketId", ticketId);
		Ticket ticket = project.findTicket(targetId);
		if (ticket == null) {
			return false;
		}
		String title = readString(payload, "title", ticket.getTitle());
		String description = readString(payload, "description", ticket.getDescription());
		String type = readString(payload, "type", ticket.getType());
		String state = readString(payload, "state", ticket.getState());
		String priorityValue = readString(payload, "priority", ticket.getPriority().name());
		TicketPriority priority;
		try {
			priority = TicketPriority.valueOf(priorityValue);
		} catch (IllegalArgumentException ignored) {
			priority = ticket.getPriority();
		}
		java.util.List<String> assigneeNames = readStringList(payload, "assigneeNames", ticket.getAssigneeNames());
		UUID sprintId = readUuid(payload, "sprintId", ticket.getSprintId());
		java.util.List<String> labels = readStringList(payload, "labels", ticket.getLabels());
		ClientPlayNetworking.send(new MossuraPayloads.UpdateTicketPayload(project.getId(), targetId, title, description, type, state, priority, assigneeNames, sprintId, labels));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		return true;
	}

	private boolean handleTicketComment(JsonObject payload) {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return false;
		}
		UUID targetId = readUuid(payload, "ticketId", ticketId);
		String message = readString(payload, "message", "").trim();
		if (message.isEmpty()) {
			return false;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddTicketCommentPayload(project.getId(), targetId, message));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		return true;
	}

	private boolean handleAddSubtask(JsonObject payload) {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return false;
		}
		UUID targetId = readUuid(payload, "ticketId", ticketId);
		String name = readString(payload, "name", "").trim();
		if (name.isEmpty()) {
			return false;
		}
		String description = readString(payload, "description", "");
		ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskPayload(project.getId(), targetId, name, description));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		return true;
	}

	private boolean handleUpdateSubtask(JsonObject payload) {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return false;
		}
		UUID targetId = readUuid(payload, "ticketId", ticketId);
		UUID subtaskId = readUuid(payload, "subtaskId", null);
		if (subtaskId == null) {
			return false;
		}
		String name = readString(payload, "name", "").trim();
		if (name.isEmpty()) {
			return false;
		}
		String description = readString(payload, "description", "");
		ClientPlayNetworking.send(new MossuraPayloads.UpdateSubtaskPayload(project.getId(), targetId, subtaskId, name, description));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		return true;
	}

	private boolean handleSubtaskComment(JsonObject payload) {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return false;
		}
		UUID targetId = readUuid(payload, "ticketId", ticketId);
		UUID subtaskId = readUuid(payload, "subtaskId", null);
		if (subtaskId == null) {
			return false;
		}
		String message = readString(payload, "message", "").trim();
		if (message.isEmpty()) {
			return false;
		}
		ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskCommentPayload(project.getId(), targetId, subtaskId, message));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		return true;
	}

	private boolean handleToggleSubtask(JsonObject payload) {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return false;
		}
		UUID targetId = readUuid(payload, "ticketId", ticketId);
		UUID subtaskId = readUuid(payload, "subtaskId", null);
		if (subtaskId == null) {
			return false;
		}
		boolean completed = payload.has("completed") && payload.get("completed").isJsonPrimitive() && payload.get("completed").getAsBoolean();
		ClientPlayNetworking.send(new MossuraPayloads.SetSubtaskCompletePayload(project.getId(), targetId, subtaskId, completed));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		return true;
	}

	private static String readString(JsonObject payload, String key, String fallback) {
		if (payload.has(key) && payload.get(key).isJsonPrimitive()) {
			return payload.get(key).getAsString();
		}
		return fallback;
	}

	private static UUID readUuid(JsonObject payload, String key, UUID fallback) {
		if (payload.has(key) && payload.get(key).isJsonPrimitive()) {
			try {
				return UUID.fromString(payload.get(key).getAsString());
			} catch (IllegalArgumentException ignored) {
				return fallback;
			}
		}
		return fallback;
	}

	private static java.util.List<String> readStringList(JsonObject payload, String key, java.util.List<String> fallback) {
		if (!payload.has(key) || !payload.get(key).isJsonArray()) {
			return new java.util.ArrayList<>(fallback);
		}
		com.google.gson.JsonArray array = payload.getAsJsonArray(key);
		java.util.List<String> values = new java.util.ArrayList<>();
		array.forEach(element -> {
			if (element.isJsonPrimitive()) {
				String value = element.getAsString().trim();
				if (!value.isEmpty()) {
					values.add(value);
				}
			}
		});
		if (values.isEmpty()) {
			return new java.util.ArrayList<>(fallback);
		}
		return values;
	}
}
