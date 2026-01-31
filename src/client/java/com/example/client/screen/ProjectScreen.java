package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.client.ui.MossuraUiState;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.screen.ProjectScreenHandler;
import com.example.tickets.TicketPriority;
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

	public ProjectScreen(ProjectScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title, "project/" + handler.getProjectId());
	}

	public UUID getProjectId() {
		return handler.getProjectId();
	}

	public void applyProjectSync(ProjectData project) {
		this.project = project;
		ClientProjectCache.put(project);
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
		return false;
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
}
