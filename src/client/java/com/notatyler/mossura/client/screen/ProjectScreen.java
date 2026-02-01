package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.network.MossuraPayloads;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.screen.ProjectScreenHandler;
import com.notatyler.mossura.tickets.TicketPriority;
import com.notatyler.mossura.tickets.Ticket;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ProjectScreen extends MossuraMcefHandledScreen<ProjectScreenHandler> {
	private ProjectData project;
	private PendingSubtasks pendingSubtasks;

	public ProjectScreen(ProjectScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title, "project/" + handler.getProjectId());
	}

	public UUID getProjectId() {
		return handler.getProjectId();
	}

	public void applyProjectSync(ProjectData project) {
		this.project = project;
		ClientProjectCache.put(project);
		flushPendingSubtasks(project);
		pushState();
	}

	@Override
	protected void init() {
		super.init();
		if (project == null) {
			project = ProjectData.fromNbt(handler.getInitialProjectNbt());
			ClientProjectCache.put(project);
		}
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		pushState();
	}

	@Override
	protected JsonObject buildState() {
		ProjectData latest = project;
		if (latest == null) {
			latest = ClientProjectCache.get(handler.getProjectId());
		}
		if (latest == null) {
			return MossuraUiState.loadingState("project");
		}
		net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
		net.minecraft.client.network.ClientPlayerEntity player = mc.player;
		long worldTicks = getWorldTicks();
		return MossuraUiState.projectState(latest, player == null ? null : player.getUuid(), player == null ? null : player.getName().getString(), System.currentTimeMillis(), worldTicks);
	}

	@Override
	public boolean handleUiAction(String action, JsonObject payload) {
		if (payload == null) {
			payload = new JsonObject();
		}
		if ("create-ticket".equals(action)) {
			return handleCreateTicket(payload);
		}
		if ("update-ticket".equals(action)) {
			return handleUpdateTicket(payload);
		}
		if ("update-project-settings".equals(action)) {
			return handleUpdateSettings(payload);
		}
		if ("open-ticket".equals(action)) {
			return handleOpenTicket(payload);
		}
		if ("add-member".equals(action)) {
			return handleAddMember(payload);
		}
		if ("remove-member".equals(action)) {
			return handleRemoveMember(payload);
		}
		if ("create-sprint".equals(action)) {
			return handleCreateSprint(payload);
		}
		if ("update-sprint".equals(action)) {
			return handleUpdateSprint(payload);
		}
		if ("add-ticket-comment".equals(action)) {
			return handleAddTicketComment(payload);
		}
		return false;
	}

	private boolean handleOpenTicket(JsonObject payload) {
		UUID targetId = readUuid(payload, "ticketId", null);
		if (targetId == null || client == null) {
			return false;
		}
		client.setScreen(new TicketViewerScreen(this, targetId));
		return true;
	}

	private boolean handleAddMember(JsonObject payload) {
		String memberName = readString(payload, "memberName", "").trim();
		if (memberName.isEmpty()) {
			return false;
		}
		String levelName = readString(payload, "level", com.notatyler.mossura.project.Member.PermissionLevel.EDITOR.name());
		com.notatyler.mossura.project.Member.PermissionLevel level = com.notatyler.mossura.project.Member.PermissionLevel.EDITOR;
		try {
			level = com.notatyler.mossura.project.Member.PermissionLevel.valueOf(levelName);
		} catch (IllegalArgumentException ignored) {}
		ClientPlayNetworking.send(new MossuraPayloads.AddProjectMemberPayload(handler.getProjectId(), memberName, level));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private boolean handleRemoveMember(JsonObject payload) {
		String memberName = readString(payload, "memberName", "").trim();
		if (memberName.isEmpty()) {
			return false;
		}
		ClientPlayNetworking.send(new MossuraPayloads.RemoveProjectMemberPayload(handler.getProjectId(), memberName));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private boolean handleCreateTicket(JsonObject payload) {
		ProjectData latest = project;
		if (latest == null) {
			latest = ClientProjectCache.get(handler.getProjectId());
		}
		if (latest == null) {
			return false;
		}
		String title = readString(payload, "title", "").trim();
		if (title.isEmpty()) {
			return false;
		}
		String description = readString(payload, "description", "");
		String type = readString(payload, "type", latest.getTicketTypes().isEmpty() ? "Task" : latest.getTicketTypes().get(0));
		String state = readString(payload, "state", latest.getStatuses().isEmpty() ? "To Do" : latest.getStatuses().get(0));
		String priorityValue = readString(payload, "priority", TicketPriority.MEDIUM.name());
		TicketPriority priority = TicketPriority.MEDIUM;
		try {
			priority = TicketPriority.valueOf(priorityValue);
		} catch (IllegalArgumentException ignored) {
			priority = TicketPriority.MEDIUM;
		}
		String assigneeName = readString(payload, "assigneeName", "");
		UUID sprintId = readUuid(payload, "sprintId", null);
		List<String> labels = readStringList(payload, "labels", new ArrayList<>());
		List<String> assignees = assigneeName.isEmpty() ? new ArrayList<>() : java.util.Collections.singletonList(assigneeName);
		ClientPlayNetworking.send(new MossuraPayloads.CreateTicketPayload(handler.getProjectId(), title, description, type, state, priority, assignees, sprintId, labels));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		List<SubtaskSpec> subtasks = readSubtasks(payload);
		if (!subtasks.isEmpty()) {
			pendingSubtasks = new PendingSubtasks(title, description, subtasks);
		}
		return true;
	}

	private boolean handleUpdateSettings(JsonObject payload) {
		ProjectData latest = project;
		if (latest == null) {
			latest = ClientProjectCache.get(handler.getProjectId());
		}
		if (latest == null) {
			return false;
		}
		String name = readString(payload, "name", latest.getName());
		String description = readString(payload, "description", latest.getDescription());
		String ticketPrefix = readString(payload, "ticketPrefix", latest.getTicketPrefix());
		List<String> statuses = readStringList(payload, "statuses", latest.getStatuses());
		List<String> types = readStringList(payload, "ticketTypes", latest.getTicketTypes());
		boolean isPublic = payload.has("isPublic") ? payload.get("isPublic").getAsBoolean() : latest.isPublic();
		ClientPlayNetworking.send(new MossuraPayloads.UpdateProjectSettingsPayload(handler.getProjectId(), name, description, ticketPrefix, statuses, types, isPublic));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private boolean handleUpdateTicket(JsonObject payload) {
		System.out.println("DEBUG: handleUpdateTicket called with payload: " + payload);
		ProjectData latest = project;
		if (latest == null) {
			latest = ClientProjectCache.get(handler.getProjectId());
		}
		if (latest == null) {
			System.out.println("DEBUG: Latest project data is null");
			return false;
		}
		UUID ticketId = readUuid(payload, "ticketId", null);
		if (ticketId == null) {
			System.out.println("DEBUG: Ticket ID is null");
			return false;
		}
		Ticket ticket = latest.findTicket(ticketId);
		if (ticket == null) {
			System.out.println("DEBUG: Ticket not found in local cache: " + ticketId);
			return false;
		}

		String title = readString(payload, "title", ticket.getTitle());
		String description = readString(payload, "description", ticket.getDescription());
		String type = readString(payload, "type", ticket.getType());
		String state = readString(payload, "state", ticket.getState());
		String priorityValue = readString(payload, "priority", ticket.getPriority().name());
		TicketPriority priority = TicketPriority.MEDIUM;
		try { priority = TicketPriority.valueOf(priorityValue); } catch (Exception ignored) { priority = ticket.getPriority(); }
		
		List<String> assignees = readStringList(payload, "assigneeNames", ticket.getAssigneeNames());
		// Fallback for singular assigneeName if present and list is empty key check
		if (!payload.has("assigneeNames") && payload.has("assigneeName")) {
			String name = readString(payload, "assigneeName", "");
			if (!name.isEmpty()) assignees = java.util.Collections.singletonList(name);
		}

		UUID sprintId = readUuid(payload, "sprintId", ticket.getSprintId());
		List<String> labels = readStringList(payload, "labels", ticket.getLabels());
		
		ClientPlayNetworking.send(new MossuraPayloads.UpdateTicketPayload(handler.getProjectId(), ticketId, title, description, type, state, priority, assignees, sprintId, labels));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private boolean handleCreateSprint(JsonObject payload) {
		String name = readString(payload, "name", "").trim();
		if (name.isEmpty()) return false;
		long start = payload.has("startTime") ? payload.get("startTime").getAsLong() : System.currentTimeMillis();
		long end = payload.has("endTime") ? payload.get("endTime").getAsLong() : start + (14L * 24 * 60 * 60 * 1000);
		ClientPlayNetworking.send(new MossuraPayloads.CreateSprintPayload(handler.getProjectId(), name, start, end));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private boolean handleUpdateSprint(JsonObject payload) {
		UUID sprintId = readUuid(payload, "sprintId", null);
		if (sprintId == null) return false;
		String name = readString(payload, "name", "");
		long start = payload.get("startTime").getAsLong();
		long end = payload.get("endTime").getAsLong();
		com.notatyler.mossura.project.Sprint.Status status = com.notatyler.mossura.project.Sprint.Status.valueOf(readString(payload, "status", "PLANNED"));
		ClientPlayNetworking.send(new MossuraPayloads.UpdateSprintPayload(handler.getProjectId(), sprintId, name, start, end, status));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private boolean handleAddTicketComment(JsonObject payload) {
		UUID ticketId = readUuid(payload, "ticketId", null);
		if (ticketId == null) return false;
		String message = readString(payload, "message", "").trim();
		if (message.isEmpty()) return false;
		ClientPlayNetworking.send(new MossuraPayloads.AddTicketCommentPayload(handler.getProjectId(), ticketId, message));
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(handler.getProjectId()));
		return true;
	}

	private static String readString(JsonObject payload, String key, String fallback) {
		if (payload.has(key) && payload.get(key).isJsonPrimitive()) {
			return payload.get(key).getAsString();
		}
		return fallback;
	}

	private static List<String> readStringList(JsonObject payload, String key, List<String> fallback) {
		if (!payload.has(key) || !payload.get(key).isJsonArray()) {
			return new ArrayList<>(fallback);
		}
		JsonArray array = payload.getAsJsonArray(key);
		List<String> values = new ArrayList<>();
		array.forEach(element -> {
			if (element.isJsonPrimitive()) {
				String value = element.getAsString().trim();
				if (!value.isEmpty()) {
					values.add(value);
				}
			}
		});
		if (values.isEmpty()) {
			return new ArrayList<>(fallback);
		}
		return values;
	}

	private static List<SubtaskSpec> readSubtasks(JsonObject payload) {
		List<SubtaskSpec> subtasks = new ArrayList<>();
		if (!payload.has("subtasks") || !payload.get("subtasks").isJsonArray()) {
			return subtasks;
		}
		payload.getAsJsonArray("subtasks").forEach(element -> {
			if (!element.isJsonObject()) {
				return;
			}
			JsonObject obj = element.getAsJsonObject();
			String name = readString(obj, "name", "").trim();
			if (name.isEmpty()) {
				return;
			}
			String description = readString(obj, "description", "");
			subtasks.add(new SubtaskSpec(name, description));
		});
		return subtasks;
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

	private void flushPendingSubtasks(ProjectData project) {
		if (pendingSubtasks == null || project == null) {
			return;
		}
		long cutoff = pendingSubtasks.requestedAt - 10000L;
		Ticket candidate = null;
		for (Ticket ticket : project.getTickets()) {
			if (ticket.isDeleted()) {
				continue;
			}
			if (!ticket.getTitle().equals(pendingSubtasks.title)) {
				continue;
			}
			if (!ticket.getDescription().equals(pendingSubtasks.description)) {
				continue;
			}
			if (ticket.getCreatedAt() < cutoff) {
				continue;
			}
			if (candidate == null || ticket.getCreatedAt() > candidate.getCreatedAt()) {
				candidate = ticket;
			}
		}
		if (candidate == null) {
			return;
		}
		for (SubtaskSpec subtask : pendingSubtasks.subtasks) {
			ClientPlayNetworking.send(new MossuraPayloads.AddSubtaskPayload(project.getId(), candidate.getId(), subtask.name, subtask.description));
		}
		ClientPlayNetworking.send(new MossuraPayloads.RequestProjectSyncPayload(project.getId()));
		pendingSubtasks = null;
	}

	private static class SubtaskSpec {
		private final String name;
		private final String description;

		private SubtaskSpec(String name, String description) {
			this.name = name;
			this.description = description;
		}
	}

	private static class PendingSubtasks {
		private final String title;
		private final String description;
		private final List<SubtaskSpec> subtasks;
		private final long requestedAt;

		private PendingSubtasks(String title, String description, List<SubtaskSpec> subtasks) {
			this.title = title;
			this.description = description;
			this.subtasks = subtasks;
			this.requestedAt = System.currentTimeMillis();
		}
	}
}
