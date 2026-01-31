package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.client.ui.MossuraUiState;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.screen.ProjectScreenHandler;
import com.example.tickets.TicketPriority;
import com.example.tickets.Ticket;
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
		return MossuraUiState.projectState(latest);
	}

	@Override
	public boolean handleUiAction(String action, JsonObject payload) {
		if (payload == null) {
			payload = new JsonObject();
		}
		if ("create-ticket".equals(action)) {
			return handleCreateTicket(payload);
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
		ClientPlayNetworking.send(new MossuraPayloads.AddProjectMemberPayload(handler.getProjectId(), memberName));
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
		ClientPlayNetworking.send(new MossuraPayloads.CreateTicketPayload(handler.getProjectId(), title, description, type, state, priority, assigneeName));
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
		List<String> statuses = readStringList(payload, "statuses", latest.getStatuses());
		List<String> types = readStringList(payload, "ticketTypes", latest.getTicketTypes());
		ClientPlayNetworking.send(new MossuraPayloads.UpdateProjectSettingsPayload(handler.getProjectId(), name, statuses, types));
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
