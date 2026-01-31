package com.example.client.ui;

import com.example.project.ProjectData;
import com.example.tickets.Comment;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;

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
		return buildState("project", project, null, null);
	}

	public static JsonObject screenState(String screen, ProjectData project) {
		return buildState(screen, project, null, null);
	}

	public static JsonObject ticketState(ProjectData project, Ticket ticket) {
		return buildState("ticket", project, ticket, null);
	}

	public static JsonObject ticketEditorState(ProjectData project, Ticket ticket) {
		return buildState(ticket == null ? "ticket-new" : "ticket-edit", project, ticket, null);
	}

	public static JsonObject projectSettingsState(ProjectData project) {
		return buildState("settings", project, null, null);
	}

	public static JsonObject ticketCommentState(ProjectData project, Ticket ticket) {
		return buildState("ticket-comment", project, ticket, null);
	}

	public static JsonObject subtaskState(ProjectData project, Ticket ticket, Subtask subtask, String screen) {
		return buildState(screen, project, ticket, subtask);
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
