package com.notatyler.mossura.project;

import com.notatyler.mossura.tickets.Comment;
import com.notatyler.mossura.tickets.Subtask;
import com.notatyler.mossura.tickets.Ticket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.UUID;

public final class MossuraUiState {
	private MossuraUiState() {
	}

	public static JsonObject loadingState(String screen) {
		JsonObject root = new JsonObject();
		root.addProperty("screen", screen);
		root.addProperty("loading", true);
		return root;
	}

	public static JsonObject projectState(ProjectData project, UUID viewerId, String viewerName) {
		JsonObject root = buildState("project", project, null, null);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static JsonObject screenState(String screen, ProjectData project, UUID viewerId, String viewerName) {
		JsonObject root = buildState(screen, project, null, null);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static JsonObject ticketState(ProjectData project, Ticket ticket, UUID viewerId, String viewerName) {
		JsonObject root = buildState("ticket", project, ticket, null);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static JsonObject ticketEditorState(ProjectData project, Ticket ticket, UUID viewerId, String viewerName) {
		JsonObject root = buildState(ticket == null ? "ticket-new" : "ticket-edit", project, ticket, null);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static JsonObject projectSettingsState(ProjectData project, UUID viewerId, String viewerName) {
		JsonObject root = buildState("settings", project, null, null);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static JsonObject ticketCommentState(ProjectData project, Ticket ticket, UUID viewerId, String viewerName) {
		JsonObject root = buildState("ticket-comment", project, ticket, null);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static JsonObject subtaskState(ProjectData project, Ticket ticket, Subtask subtask, String screen, UUID viewerId, String viewerName) {
		JsonObject root = buildState(screen, project, ticket, subtask);
		applyPermissions(root, project, viewerId, viewerName);
		return root;
	}

	public static void applyPermissions(JsonObject root, ProjectData project, UUID viewerId, String viewerName) {
		if (root == null || project == null) {
			return;
		}
		
		boolean isOwner = viewerId != null && viewerId.equals(project.getOwnerId());
		com.notatyler.mossura.project.Member member = viewerId == null ? null : project.getMembersMap().get(viewerId);
		com.notatyler.mossura.project.Member.PermissionLevel level = member == null ? null : member.getPermissionLevel();
		
		root.addProperty("isOwner", isOwner);
		root.addProperty("isAdmin", isOwner || level == com.notatyler.mossura.project.Member.PermissionLevel.ADMIN);
		root.addProperty("canEdit", isOwner || level == com.notatyler.mossura.project.Member.PermissionLevel.ADMIN || level == com.notatyler.mossura.project.Member.PermissionLevel.EDITOR);
		root.addProperty("isMember", isOwner || level != null);
		root.addProperty("playerName", viewerName == null ? "Guest" : viewerName);
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
		obj.addProperty("description", project.getDescription() == null ? "" : project.getDescription());
		obj.addProperty("ownerName", project.getOwnerName());
		obj.addProperty("ticketPrefix", project.getTicketPrefix());
		obj.addProperty("isPublic", project.isPublic());
		JsonArray members = new JsonArray();
		for (com.notatyler.mossura.project.Member member : project.getMembersMap().values()) {
			JsonObject memberJson = new JsonObject();
			memberJson.addProperty("id", member.getUuid().toString());
			memberJson.addProperty("name", member.getName());
			memberJson.addProperty("permission", member.getPermissionLevel().name());
			members.add(memberJson);
		}
		obj.add("members", members);
		JsonArray sprints = new JsonArray();
		for (com.notatyler.mossura.project.Sprint sprint : project.getSprints()) {
			JsonObject sprintJson = new JsonObject();
			sprintJson.addProperty("id", sprint.getId().toString());
			sprintJson.addProperty("name", sprint.getName());
			sprintJson.addProperty("startTime", sprint.getStartTime());
			sprintJson.addProperty("endTime", sprint.getEndTime());
			sprintJson.addProperty("status", sprint.getStatus().name());
			sprints.add(sprintJson);
		}
		obj.add("sprints", sprints);
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
		
		JsonArray assignees = new JsonArray();
		for (String name : ticket.getAssigneeNames()) {
			assignees.add(name);
		}
		obj.add("assignees", assignees);
		// Backwards compatibility for now
		obj.addProperty("assigneeName", ticket.getAssigneeNames().isEmpty() ? "" : ticket.getAssigneeNames().get(0));
		
		obj.addProperty("creatorName", ticket.getCreatorName());
		obj.addProperty("createdAt", ticket.getCreatedAt());
		if (ticket.getSprintId() != null) {
			obj.addProperty("sprintId", ticket.getSprintId().toString());
		}
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
		JsonArray labels = new JsonArray();
		for (String label : ticket.getLabels()) {
			labels.add(label);
		}
		obj.add("labels", labels);
		JsonArray history = new JsonArray();
		for (com.notatyler.mossura.tickets.HistoryEntry entry : ticket.getHistory()) {
			JsonObject entryJson = new JsonObject();
			entryJson.addProperty("id", entry.getId().toString());
			entryJson.addProperty("actorName", entry.getActorName());
			entryJson.addProperty("action", entry.getAction());
			entryJson.addProperty("field", entry.getField());
			entryJson.addProperty("before", entry.getBeforeValue());
			entryJson.addProperty("after", entry.getAfterValue());
			entryJson.addProperty("timestamp", entry.getTimestamp());
			history.add(entryJson);
		}
		obj.add("history", history);
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
