package com.notatyler.mossura.tickets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.notatyler.mossura.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class Ticket {
	private final UUID id;
	private final int number;
	private final UUID creatorId;
	private final String creatorName;
	private String title;
	private String description;
	private TicketPriority priority;
	private String type;
	private String state;
	private final List<UUID> assigneeIds = new ArrayList<>();
	private final List<String> assigneeNames = new ArrayList<>();
	private final long createdAt;
	private UUID sprintId;
	private boolean deleted;
	private final List<String> labels = new ArrayList<>();
	private final List<UUID> relatedTickets = new ArrayList<>();
	private final List<Subtask> subtasks = new ArrayList<>();
	private final List<Comment> comments = new ArrayList<>();
	private final List<HistoryEntry> history = new ArrayList<>();

	public Ticket(UUID id, int number, UUID creatorId, String creatorName, String title, String description, TicketPriority priority, String type, String state, List<UUID> assigneeIds, List<String> assigneeNames, long createdAt) {
		this.id = id;
		this.number = number;
		this.creatorId = creatorId;
		this.creatorName = creatorName;
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.type = type;
		this.state = state;
		this.state = state;
		if (assigneeIds != null) this.assigneeIds.addAll(assigneeIds);
		if (assigneeNames != null) this.assigneeNames.addAll(assigneeNames);
		this.createdAt = createdAt;
	}

	public static Ticket create(int number, UUID creatorId, String creatorName, String title, String description, TicketPriority priority, String type, String state, List<UUID> assigneeIds, List<String> assigneeNames, long createdAt) {
		return new Ticket(UUID.randomUUID(), number, creatorId, creatorName, title, description, priority, type, state, assigneeIds, assigneeNames, createdAt);
	}

	public UUID getId() {
		return id;
	}

	public int getNumber() {
		return number;
	}

	public UUID getCreatorId() {
		return creatorId;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TicketPriority getPriority() {
		return priority;
	}

	public void setPriority(TicketPriority priority) {
		this.priority = priority;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public List<UUID> getAssigneeIds() {
		return Collections.unmodifiableList(assigneeIds);
	}

	public void setAssigneeIds(List<UUID> assigneeIds) {
		this.assigneeIds.clear();
		if (assigneeIds != null) {
			this.assigneeIds.addAll(assigneeIds);
		}
	}

	public List<String> getAssigneeNames() {
		return Collections.unmodifiableList(assigneeNames);
	}

	public void setAssigneeNames(List<String> assigneeNames) {
		this.assigneeNames.clear();
		if (assigneeNames != null) {
			this.assigneeNames.addAll(assigneeNames);
		}
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public UUID getSprintId() {
		return sprintId;
	}

	public void setSprintId(UUID sprintId) {
		this.sprintId = sprintId;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public List<String> getLabels() {
		return Collections.unmodifiableList(labels);
	}

	public void addLabel(String label) {
		if (!labels.contains(label)) {
			labels.add(label);
		}
	}

	public void removeLabel(String label) {
		labels.remove(label);
	}

	public List<UUID> getRelatedTickets() {
		return Collections.unmodifiableList(relatedTickets);
	}

	public void addRelatedTicket(UUID ticketId) {
		if (!relatedTickets.contains(ticketId)) {
			relatedTickets.add(ticketId);
		}
	}

	public void removeRelatedTicket(UUID ticketId) {
		relatedTickets.remove(ticketId);
	}

	public List<Subtask> getSubtasks() {
		return Collections.unmodifiableList(subtasks);
	}

	public void addSubtask(Subtask subtask) {
		subtasks.add(subtask);
	}

	public List<Comment> getComments() {
		return Collections.unmodifiableList(comments);
	}

	public void addComment(Comment comment) {
		comments.add(comment);
	}

	public List<HistoryEntry> getHistory() {
		return Collections.unmodifiableList(history);
	}

	public void addHistory(HistoryEntry entry) {
		history.add(entry);
	}

	public Subtask findSubtask(UUID subtaskId) {
		for (Subtask subtask : subtasks) {
			if (subtask.getId().equals(subtaskId)) {
				return subtask;
			}
		}
		return null;
	}

	public boolean matches(UUID ticketId) {
		return Objects.equals(id, ticketId);
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtUtil.putUuid(nbt, "id", id);
		nbt.putInt("number", number);
		NbtUtil.putUuid(nbt, "creatorId", creatorId);
		nbt.putString("creatorName", creatorName);
		nbt.putString("title", title);
		nbt.putString("description", description);
		nbt.putString("priority", priority.name());
		nbt.putString("type", type);
		nbt.putString("state", state);
		nbt.putString("type", type);
		nbt.putString("state", state);
		
		NbtList assigneeIdList = new NbtList();
		for (UUID uuid : assigneeIds) {
			NbtCompound idNbt = new NbtCompound();
			NbtUtil.putUuid(idNbt, "id", uuid);
			assigneeIdList.add(idNbt);
		}
		nbt.put("assigneeIds", assigneeIdList);

		NbtList assigneeNameList = new NbtList();
		for (String name : assigneeNames) {
			assigneeNameList.add(net.minecraft.nbt.NbtString.of(name));
		}
		nbt.put("assigneeNames", assigneeNameList);

		nbt.putLong("createdAt", createdAt);
		if (deleted) {
			nbt.putBoolean("deleted", true);
		}
		NbtList subtaskList = new NbtList();
		for (Subtask subtask : subtasks) {
			subtaskList.add(subtask.toNbt());
		}
		nbt.put("subtasks", subtaskList);
		NbtList commentList = new NbtList();
		for (Comment comment : comments) {
			commentList.add(comment.toNbt());
		}
		nbt.put("comments", commentList);
		NbtList historyList = new NbtList();
		for (HistoryEntry entry : history) {
			historyList.add(entry.toNbt());
		}
		if (sprintId != null) {
			NbtUtil.putUuid(nbt, "sprintId", sprintId);
		}
		NbtList labelList = new NbtList();
		for (String label : labels) {
			labelList.add(net.minecraft.nbt.NbtString.of(label));
		}
		nbt.put("labels", labelList);
		NbtList relatedList = new NbtList();
		for (UUID id : relatedTickets) {
			NbtCompound idNbt = new NbtCompound();
			NbtUtil.putUuid(idNbt, "id", id);
			relatedList.add(idNbt);
		}
		nbt.put("relatedTickets", relatedList);
		nbt.put("history", historyList);
		return nbt;
	}

	public static Ticket fromNbt(NbtCompound nbt) {
		UUID id = NbtUtil.getUuid(nbt, "id");
		int number = nbt.getInt("number", 0);
		UUID creatorId = NbtUtil.getUuid(nbt, "creatorId");
		String creatorName = nbt.getString("creatorName", "");
		String title = nbt.getString("title", "");
		String description = nbt.getString("description", "");
		TicketPriority priority = TicketPriority.MEDIUM;
		String priorityValue = nbt.getString("priority", "");
		if (!priorityValue.isEmpty()) {
			try {
				priority = TicketPriority.valueOf(priorityValue);
			} catch (IllegalArgumentException ignored) {
				priority = TicketPriority.MEDIUM;
			}
		}
		String type = nbt.getString("type", "");
		String state = nbt.getString("state", "");
		
		List<UUID> assigneeIds = new ArrayList<>();
		NbtList ids = nbt.getListOrEmpty("assigneeIds");
		for (int i = 0; i < ids.size(); i++) {
			assigneeIds.add(NbtUtil.getUuid(ids.getCompoundOrEmpty(i), "id"));
		}
		// Migrating old format
		if (assigneeIds.isEmpty() && nbt.contains("assigneeId")) {
			assigneeIds.add(NbtUtil.getUuid(nbt, "assigneeId"));
		}

		List<String> assigneeNames = new ArrayList<>();
		NbtList names = nbt.getListOrEmpty("assigneeNames");
		for (int i = 0; i < names.size(); i++) {
			assigneeNames.add(names.getString(i).orElse(""));
		}
		// Migrating old format
		if (assigneeNames.isEmpty() && nbt.contains("assigneeName")) {
			assigneeNames.add(nbt.getString("assigneeName").orElse(""));
		}

		long createdAt = nbt.getLong("createdAt", 0L);
		Ticket ticket = new Ticket(id, number, creatorId, creatorName, title, description, priority, type, state, assigneeIds, assigneeNames, createdAt);
		ticket.sprintId = NbtUtil.getUuid(nbt, "sprintId");
		ticket.deleted = nbt.getBoolean("deleted", false);
		NbtList subtaskList = nbt.getListOrEmpty("subtasks");
		for (int i = 0; i < subtaskList.size(); i++) {
			ticket.subtasks.add(Subtask.fromNbt(subtaskList.getCompoundOrEmpty(i)));
		}
		NbtList commentList = nbt.getListOrEmpty("comments");
		for (int i = 0; i < commentList.size(); i++) {
			ticket.comments.add(Comment.fromNbt(commentList.getCompoundOrEmpty(i)));
		}
		NbtList historyList = nbt.getListOrEmpty("history");
		for (int i = 0; i < historyList.size(); i++) {
			ticket.addHistory(HistoryEntry.fromNbt(historyList.getCompoundOrEmpty(i)));
		}
		
		NbtList labels = nbt.getListOrEmpty("labels");
		for (int i = 0; i < labels.size(); i++) {
			ticket.labels.add(labels.getString(i, ""));
		}

		NbtList related = nbt.getListOrEmpty("relatedTickets");
		for (int i = 0; i < related.size(); i++) {
			ticket.relatedTickets.add(NbtUtil.getUuid(related.getCompoundOrEmpty(i), "id"));
		}
		return ticket;
	}
}
