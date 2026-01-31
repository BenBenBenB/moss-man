package com.example.client.ui;

import com.example.project.ProjectData;
import com.example.tickets.Comment;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;

public final class MossuraUiState {
	private MossuraUiState() {
	}

	public static JsonObject loadingState(String screen) {
		JsonObject root = new JsonObject();
		root.addProperty("screen", screen);
		root.addProperty("loading", true);
		return root;
	}

	public static JsonObject projectState(ProjectData project) {
		JsonObject root = buildState("project", project, null, null);
		applyPermissions(root, project);
		return root;
	}

	public static JsonObject screenState(String screen, ProjectData project) {
		JsonObject root = buildState(screen, project, null, null);
		applyPermissions(root, project);
		return root;
	}

	public static JsonObject ticketState(ProjectData project, Ticket ticket) {
		JsonObject root = buildState("ticket", project, ticket, null);
		applyPermissions(root, project);
		return root;
	}

	public static JsonObject ticketEditorState(ProjectData project, Ticket ticket) {
		JsonObject root = buildState(ticket == null ? "ticket-new" : "ticket-edit", project, ticket, null);
		applyPermissions(root, project);
		return root;
	}

	public static JsonObject projectSettingsState(ProjectData project) {
		JsonObject root = buildState("settings", project, null, null);
		applyPermissions(root, project);
		return root;
	}

	public static JsonObject ticketCommentState(ProjectData project, Ticket ticket) {
		JsonObject root = buildState("ticket-comment", project, ticket, null);
		applyPermissions(root, project);
		return root;
	}

	public static JsonObject subtaskState(ProjectData project, Ticket ticket, Subtask subtask, String screen) {
		JsonObject root = buildState(screen, project, ticket, subtask);
		applyPermissions(root, project);
		return root;
	}

	public static void applyPermissions(JsonObject root, ProjectData project) {
		if (root == null || project == null) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		UUID playerId = client.player.getUuid();
		boolean isOwner = playerId.equals(project.getOwnerId());
		boolean isMember = project.getMembers().containsKey(playerId);
		root.addProperty("isOwner", isOwner);
		root.addProperty("canEdit", isOwner || isMember);
		root.addProperty("playerName", client.player.getName().getString());
	}

	private static JsonObject buildState(String screen, ProjectData project, Ticket ticket, Subtask subtask) {
		JsonObject root = new JsonObject();
		root.addProperty("screen", screen);
		root.addProperty("updatedAt", System.currentTimeMillis());
		if (project != null) {
			root.add("project", projectToJson(project));
		} else {
			root.addProperty("loading", true);
		}
		if (ticket != null) {
			root.add("ticket", ticketToJson(ticket));
		}
		if (subtask != null) {
			root.add("subtask", subtaskToJson(subtask));
		}
		return root;
	}

	private static JsonObject projectToJson(ProjectData project) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", project.getId().toString());
		obj.addProperty("name", project.getName());
		obj.addProperty("ownerName", project.getOwnerName());
		JsonArray members = new JsonArray();
		for (Map.Entry<java.util.UUID, String> entry : project.getMembers().entrySet()) {
			JsonObject member = new JsonObject();
			member.addProperty("id", entry.getKey().toString());
			member.addProperty("name", entry.getValue());
			members.add(member);
		}
		obj.add("members", members);
		JsonArray statuses = new JsonArray();
		for (String status : project.getStatuses()) {
			statuses.add(status);
		}
		obj.add("statuses", statuses);
		JsonArray types = new JsonArray();
		for (String type : project.getTicketTypes()) {
			types.add(type);
		}
		obj.add("ticketTypes", types);
		JsonArray tickets = new JsonArray();
		for (Ticket ticket : project.getTickets()) {
			if (ticket.isDeleted()) {
				continue;
			}
			tickets.add(ticketToJson(ticket));
		}
		obj.add("tickets", tickets);
		obj.addProperty("ticketCount", tickets.size());
		return obj;
	}

	private static JsonObject ticketToJson(Ticket ticket) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", ticket.getId().toString());
		obj.addProperty("number", ticket.getNumber());
		obj.addProperty("title", ticket.getTitle());
		obj.addProperty("description", ticket.getDescription());
		obj.addProperty("priority", ticket.getPriority().name());
		obj.addProperty("type", ticket.getType());
		obj.addProperty("state", ticket.getState());
		obj.addProperty("assigneeName", ticket.getAssigneeName() == null ? "" : ticket.getAssigneeName());
		obj.addProperty("creatorName", ticket.getCreatorName());
		obj.addProperty("createdAt", ticket.getCreatedAt());
		JsonArray subtasks = new JsonArray();
		for (Subtask subtask : ticket.getSubtasks()) {
			subtasks.add(subtaskToJson(subtask));
		}
		obj.add("subtasks", subtasks);
		JsonArray comments = new JsonArray();
		for (Comment comment : ticket.getComments()) {
			comments.add(commentToJson(comment));
		}
		obj.add("comments", comments);
		return obj;
	}

	private static JsonObject subtaskToJson(Subtask subtask) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", subtask.getId().toString());
		obj.addProperty("name", subtask.getName());
		obj.addProperty("description", subtask.getDescription());
		obj.addProperty("creatorName", subtask.getCreatorName());
		obj.addProperty("createdAt", subtask.getCreatedAt());
		obj.addProperty("completed", subtask.isCompleted());
		JsonArray comments = new JsonArray();
		for (Comment comment : subtask.getComments()) {
			comments.add(commentToJson(comment));
		}
		obj.add("comments", comments);
		return obj;
	}

	private static JsonObject commentToJson(Comment comment) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", comment.getId().toString());
		obj.addProperty("authorName", comment.getAuthorName());
		obj.addProperty("message", comment.getMessage());
		obj.addProperty("createdAt", comment.getCreatedAt());
		return obj;
	}
}
