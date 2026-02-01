package com.notatyler.mossura.project;

import com.notatyler.mossura.tickets.Comment;
import com.notatyler.mossura.tickets.HistoryEntry;
import com.notatyler.mossura.tickets.Subtask;
import com.notatyler.mossura.tickets.Ticket;
import com.notatyler.mossura.tickets.TicketPriority;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.notatyler.mossura.notification.NotificationManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ProjectService {
	private ProjectService() {
	}

	public static Ticket createTicket(MinecraftServer server, ProjectData project, UUID actorId, String actorName, String title, String description, TicketPriority priority, String type, String state, List<UUID> assigneeIds, List<String> assigneeNames, long timestamp) {
		int number = project.allocateTicketNumber();
		Ticket ticket = Ticket.create(number, actorId, actorName, title, description, priority, type, state, assigneeIds, assigneeNames, timestamp);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "create", "ticket", "", "", "ticket", ticket.getId(), timestamp));
		project.addTicket(ticket);
		
		if (assigneeIds != null) {
			for (UUID assigneeId : assigneeIds) {
				sendAssignmentNotification(server, project, ticket, assigneeId, actorName);
			}
		}
		
		return ticket;
	}

	public static void updateTicket(MinecraftServer server, ProjectData project, Ticket ticket, UUID actorId, String actorName, TicketUpdate update, long timestamp) {
		applyFieldChange(ticket.getTitle(), update.title(), "title", actorId, actorName, ticket, timestamp, ticket::setTitle);
		applyFieldChange(ticket.getDescription(), update.description(), "description", actorId, actorName, ticket, timestamp, ticket::setDescription);
		applyFieldChange(ticket.getPriority() == null ? null : ticket.getPriority().name(),
				update.priority() == null ? null : update.priority().name(),
				"priority", actorId, actorName, ticket, timestamp, value -> ticket.setPriority(update.priority()));
		applyFieldChange(ticket.getType(), update.type(), "type", actorId, actorName, ticket, timestamp, ticket::setType);
		applyFieldChange(ticket.getState(), update.state(), "state", actorId, actorName, ticket, timestamp, ticket::setState);

		updateAssignees(server, project, ticket, actorId, actorName, update.assigneeIds(), update.assigneeNames(), timestamp);

		if (!Objects.equals(ticket.getSprintId(), update.sprintId())) {
			String before = "None";
			if (ticket.getSprintId() != null) {
				Sprint s = project.findSprint(ticket.getSprintId());
				before = s != null ? s.getName() : "Unknown";
			}
			String after = "None";
			if (update.sprintId() != null) {
				Sprint s = project.findSprint(update.sprintId());
				after = s != null ? s.getName() : "Unknown";
			}
			ticket.setSprintId(update.sprintId());
			ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "sprint", before, after, "ticket", ticket.getId(), timestamp));
		}

		if (update.labels() != null) {
			List<String> beforeLabels = ticket.getLabels();
			if (!Objects.equals(beforeLabels, update.labels())) {
				String before = String.join(", ", beforeLabels);
				String after = String.join(", ", update.labels());
				
				List<String> current = new java.util.ArrayList<>(ticket.getLabels());
				current.forEach(ticket::removeLabel);
				update.labels().forEach(ticket::addLabel);
				
				ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "labels", before, after, "ticket", ticket.getId(), timestamp));
			}
		}
	}

	private static void sendAssignmentNotification(MinecraftServer server, ProjectData project, Ticket ticket, UUID assigneeId, String actorName) {
		String prefix = project.getTicketPrefix();
		int num = ticket.getNumber();
		String title = ticket.getTitle();
		
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(assigneeId);
		if (player != null) {
			String message = String.format("[Mossura] You've been assigned to %s-%d: %s", prefix, num, title);
			player.sendMessage(Text.literal(message).formatted(Formatting.GREEN), false);
		} else {
			String message = String.format("[Mossura] You were assigned to %s-%d: %s while you were away", prefix, num, title);
			NotificationManager.get(server).addAssignmentNotification(assigneeId, ticket.getId(), message);
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

	public static void updateProjectSettings(MinecraftServer server, ProjectData project, String name, String description, String ticketPrefix, List<String> statuses, List<String> ticketTypes, long timestamp) {
		boolean wasNotExample = !project.getName().equals("EXAMPLE");
		if (name != null && !name.isBlank()) {
			project.setName(name);
		}
		if (description != null) {
			project.setDescription(description);
		}
		if (ticketPrefix != null && !ticketPrefix.isBlank()) {
			project.setTicketPrefix(ticketPrefix);
		}
		if (statuses != null && !statuses.isEmpty()) {
			project.setStatuses(statuses);
		}
		if (ticketTypes != null && !ticketTypes.isEmpty()) {
			project.setTicketTypes(ticketTypes);
		}
		
		// Auto-populate example data when renamed to EXAMPLE
		if (wasNotExample && "EXAMPLE".equals(project.getName()) && project.getTickets().isEmpty()) {
			populateExampleData(server, project, timestamp);
		}
	}

	public static void addMember(ProjectData project, UUID memberId, String memberName, Member.PermissionLevel level) {
		project.addMember(memberId, memberName, level);
	}

	public static void removeMember(ProjectData project, UUID memberId) {
		project.removeMember(memberId);
	}

	public static void updateProjectPublic(ProjectData project, boolean isPublic) {
		project.setPublic(isPublic);
	}

	public static void addSprint(ProjectData project, String name, long startTime, long endTime) {
		project.addSprint(Sprint.create(name, startTime, endTime));
	}

	public static void updateSprint(ProjectData project, UUID sprintId, String name, long startTime, long endTime, Sprint.Status status) {
		Sprint sprint = project.findSprint(sprintId);
		if (sprint != null) {
			sprint.setName(name);
			sprint.setStartTime(startTime);
			sprint.setEndTime(endTime);
			sprint.setStatus(status);
		}
	}

	public static void deleteTicket(ProjectData project, Ticket ticket, UUID actorId, String actorName, long timestamp) {
		if (ticket.isDeleted()) {
			return;
		}
		ticket.setDeleted(true);
		ticket.addHistory(HistoryEntry.create(actorId, actorName, "delete", "ticket", "", "deleted", "ticket", ticket.getId(), timestamp));
	}

	private static void updateAssignees(MinecraftServer server, ProjectData project, Ticket ticket, UUID actorId, String actorName, List<UUID> newIds, List<String> newNames, long timestamp) {
		List<UUID> currentIds = ticket.getAssigneeIds();
		if (!Objects.equals(currentIds, newIds)) {
			String before = String.join(", ", ticket.getAssigneeNames());
			String after = String.join(", ", newNames != null ? newNames : List.of());
			
			// Cancel notifications for removed assignees
			for (UUID oldId : currentIds) {
				if (newIds == null || !newIds.contains(oldId)) {
					NotificationManager.get(server).cancelAssignmentNotification(oldId, ticket.getId());
				}
			}

			ticket.setAssigneeIds(newIds);
			ticket.setAssigneeNames(newNames);
			ticket.addHistory(HistoryEntry.create(actorId, actorName, "update", "assignees", before, after, "ticket", ticket.getId(), timestamp));
			
			// Send notifications to new assignees
			if (newIds != null) {
				for (UUID newId : newIds) {
					if (!currentIds.contains(newId)) {
						sendAssignmentNotification(server, project, ticket, newId, actorName);
					}
				}
			}
		}
	}

	private static void populateExampleData(MinecraftServer server, ProjectData project, long timestamp) {
		UUID systemId = UUID.fromString("00000000-0000-0000-0000-000000000000");
		String systemName = "System";
		
		// Create LV Age sprint
		Sprint lvSprint = Sprint.create("LV Age Progression", timestamp, timestamp + (14L * 24 * 60 * 60 * 1000));
		lvSprint.setStatus(Sprint.Status.ACTIVE);
		project.addSprint(lvSprint);
		
		// Create example tickets for GregTech LV Age
		Ticket t1 = createTicket(server, project, systemId, systemName,
			"Craft Steel Ingots",
			"Smelt iron dust with coal dust in the primitive blast furnace to produce steel ingots. You'll need at least 64 steel ingots for basic LV machines.",
			TicketPriority.HIGH, "Story", "In Progress",
			null, List.of(), timestamp);
		t1.setSprintId(lvSprint.getId());
		t1.addLabel("materials");
		t1.addLabel("progression");
		
		Ticket t2 = createTicket(server, project, systemId, systemName,
			"Craft LV Machine Hull",
			"Combine steel plates with LV circuits to create the LV machine hull. This is the foundation for all LV-tier machines.",
			TicketPriority.MEDIUM, "Task", "To Do",
			null, List.of(), timestamp);
		t2.setSprintId(lvSprint.getId());
		t2.addLabel("crafting");
		
		Ticket t3 = createTicket(server, project, systemId, systemName,
			"Build Electric Blast Furnace",
			"Construct your first Electric Blast Furnace (EBF) using LV machine hulls and heating coils. This unlocks aluminum and titanium processing.",
			TicketPriority.HIGH, "Quest", "To Do",
			null, List.of(), timestamp);
		t3.setSprintId(lvSprint.getId());
		t3.addLabel("multiblock");
		t3.addLabel("boss fight");
		
		Ticket t4 = createTicket(server, project, systemId, systemName,
			"Automate Steam Production",
			"Set up automated steam generation using solar boilers or coal-fired boilers to support your LV machines.",
			TicketPriority.LOW, "Task", "Done",
			null, List.of(), timestamp);
		t4.setSprintId(lvSprint.getId());
		t4.addLabel("automation");
		
		Ticket t5 = createTicket(server, project, systemId, systemName,
			"Craft Basic Electronic Circuit",
			"Assemble basic electronic circuits using resistors, vacuum tubes, and copper cables. Required for LV machine upgrades.",
			TicketPriority.MEDIUM, "Task", "To Do",
			null, List.of(), timestamp);
		t5.setSprintId(lvSprint.getId());
		t5.addLabel("crafting");
		t5.addLabel("resources");
		
		// Add subtasks to the EBF quest
		addSubtask(t3, systemId, systemName, "Gather 32 Steel Ingots", "Mine and process iron to create steel ingots", timestamp);
		addSubtask(t3, systemId, systemName, "Craft Heating Coils", "Create cupronickel heating coils for the EBF", timestamp);
		addSubtask(t3, systemId, systemName, "Assemble Multiblock", "Place blocks in 3x3x4 structure and validate with wrench", timestamp);
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
