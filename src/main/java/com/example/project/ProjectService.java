package com.example.project;

import com.example.tickets.Comment;
import com.example.tickets.HistoryEntry;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
import com.example.tickets.TicketPriority;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProjectService {
	private ProjectService() {
	}

	public static Ticket createTicket(ProjectData project, UUID actorId, String actorName, String title, String description, TicketPriority priority, String type, String state, UUID assigneeId, String assigneeName, long timestamp) {
		int number = project.allocateTicketNumber();
		Ticket ticket = Ticket.create(number, actorId, actorName, title, description, priority, type, state, assigneeId, assigneeName, timestamp);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "create", "ticket", "", "", "ticket", ticket.getId(), timestamp));
		project.addTicket(ticket);
		return ticket;
	}

	public static void updateTicket(ProjectData project, Ticket ticket, UUID actorId, String actorName, TicketUpdate update, long timestamp) {
		applyFieldChange(ticket.getTitle(), update.title(), "title", actorId, actorName, ticket, timestamp, ticket::setTitle);
		applyFieldChange(ticket.getDescription(), update.description(), "description", actorId, actorName, ticket, timestamp, ticket::setDescription);
		applyFieldChange(ticket.getPriority() == null ? null : ticket.getPriority().name(),
				update.priority() == null ? null : update.priority().name(),
				"priority", actorId, actorName, ticket, timestamp, value -> ticket.setPriority(update.priority()));
		applyFieldChange(ticket.getType(), update.type(), "type", actorId, actorName, ticket, timestamp, ticket::setType);
		applyFieldChange(ticket.getState(), update.state(), "state", actorId, actorName, ticket, timestamp, ticket::setState);

		if (!Objects.equals(ticket.getAssigneeId(), update.assigneeId()) || !Objects.equals(ticket.getAssigneeName(), update.assigneeName())) {
			String before = ticket.getAssigneeName() == null ? "" : ticket.getAssigneeName();
			String after = update.assigneeName() == null ? "" : update.assigneeName();
			ticket.setAssigneeId(update.assigneeId());
			ticket.setAssigneeName(update.assigneeName());
			ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "assignee", before, after, "ticket", ticket.getId(), timestamp));
		}
	}

	public static Comment addTicketComment(Ticket ticket, UUID actorId, String actorName, String message, long timestamp) {
		Comment comment = Comment.create(actorId, actorName, message, timestamp);
		ticket.addComment(comment);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "comment", "ticket", "", message, "comment", comment.getId(), timestamp));
		return comment;
	}

	public static Subtask addSubtask(Ticket ticket, UUID actorId, String actorName, String name, String description, long timestamp) {
		Subtask subtask = Subtask.create(actorId, actorName, name, description, timestamp);
		ticket.addSubtask(subtask);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "subtask_add", "subtask", "", name, "subtask", subtask.getId(), timestamp));
		return subtask;
	}

	public static Comment addSubtaskComment(Ticket ticket, Subtask subtask, UUID actorId, String actorName, String message, long timestamp) {
		Comment comment = Comment.create(actorId, actorName, message, timestamp);
		subtask.addComment(comment);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "comment", "subtask", "", message, "subtask_comment", comment.getId(), timestamp));
		return comment;
	}

	public static void updateSubtask(Ticket ticket, Subtask subtask, UUID actorId, String actorName, String name, String description, long timestamp) {
		if (!Objects.equals(subtask.getName(), name)) {
			String before = subtask.getName();
			subtask.setName(name);
			ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "subtask_name", before, name, "subtask", subtask.getId(), timestamp));
		}
		if (!Objects.equals(subtask.getDescription(), description)) {
			String before = subtask.getDescription();
			subtask.setDescription(description);
			ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "subtask_description", before, description, "subtask", subtask.getId(), timestamp));
		}
	}

	public static void setSubtaskCompleted(Ticket ticket, Subtask subtask, UUID actorId, String actorName, boolean completed, long timestamp) {
		if (subtask.isCompleted() == completed) {
			return;
		}
		String before = subtask.isCompleted() ? "complete" : "incomplete";
		String after = completed ? "complete" : "incomplete";
		subtask.setCompleted(completed);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "subtask_status", before, after, "subtask", subtask.getId(), timestamp));
	}

	public static void updateProjectSettings(ProjectData project, String name, List<String> statuses, List<String> ticketTypes) {
		if (name != null && !name.isBlank()) {
			project.setName(name);
		}
		if (statuses != null && !statuses.isEmpty()) {
			project.setStatuses(statuses);
		}
		if (ticketTypes != null && !ticketTypes.isEmpty()) {
			project.setTicketTypes(ticketTypes);
		}
	}

	public static void addMember(ProjectData project, UUID memberId, String memberName) {
		project.addMember(memberId, memberName);
	}

	public static void removeMember(ProjectData project, UUID memberId) {
		project.removeMember(memberId);
	}

	public static void deleteTicket(ProjectData project, Ticket ticket, UUID actorId, String actorName, long timestamp) {
		if (ticket.isDeleted()) {
			return;
		}
		ticket.setDeleted(true);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "delete", "ticket", "", "deleted", "ticket", ticket.getId(), timestamp));
	}

	private static void applyFieldChange(String before, String after, String field, UUID actorId, String actorName, Ticket ticket, long timestamp, java.util.function.Consumer<String> setter) {
		if (!Objects.equals(before, after)) {
			String safeBefore = before == null ? "" : before;
			String safeAfter = after == null ? "" : after;
			setter.accept(after);
			ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", field, safeBefore, safeAfter, "ticket", ticket.getId(), timestamp));
		}
	}
}
