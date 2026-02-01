package com.notatyler.mossura.project;

import com.notatyler.mossura.tickets.Ticket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.notatyler.mossura.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class ProjectData {
	private final UUID id;
	private String name;
	private final UUID ownerId;
	private String ownerName;
	private final Map<UUID, Member> members = new LinkedHashMap<>();
	private final List<Sprint> sprints = new ArrayList<>();
	private String description;
	private final List<String> statuses = new ArrayList<>();
	private final List<String> ticketTypes = new ArrayList<>();
	private int nextTicketNumber = 1000;
	private String ticketPrefix;
	private final List<Ticket> tickets = new ArrayList<>();

	public ProjectData(UUID id, String name, String description, UUID ownerId, String ownerName, String ticketPrefix) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
		this.ticketPrefix = validateTicketPrefix(ticketPrefix);
	}

	private String validateTicketPrefix(String prefix) {
		if (prefix == null || prefix.length() < 2) {
			prefix = "PRJ";
		}
		return prefix.toUpperCase();
	}

	public static ProjectData create(UUID ownerId, String ownerName, String name, String prefix) {
		ProjectData data = new ProjectData(UUID.randomUUID(), name, "", ownerId, ownerName, prefix);
		data.members.put(ownerId, new Member(ownerId, ownerName, Member.PermissionLevel.ADMIN));
		data.statuses.addAll(List.of("To Do", "In Progress", "Done", "Blocked"));
		data.ticketTypes.addAll(List.of("Epic", "Story", "Task"));
		return data;
	}

	public String getTicketPrefix() {
		return ticketPrefix;
	}

	public void setTicketPrefix(String ticketPrefix) {
		this.ticketPrefix = validateTicketPrefix(ticketPrefix);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public Map<UUID, Member> getMembersMap() {
		return Collections.unmodifiableMap(members);
	}

	public void addMember(UUID memberId, String memberName, Member.PermissionLevel level) {
		members.put(memberId, new Member(memberId, memberName, level));
	}

	public void removeMember(UUID memberId) {
		members.remove(memberId);
	}

	public UUID findMemberIdByName(String name) {
		for (Member member : members.values()) {
			if (member.getName().equalsIgnoreCase(name)) {
				return member.getUuid();
			}
		}
		return null;
	}

	public List<Sprint> getSprints() {
		return Collections.unmodifiableList(sprints);
	}

	public void addSprint(Sprint sprint) {
		sprints.add(sprint);
	}

	public Sprint findSprint(UUID sprintId) {
		for (Sprint sprint : sprints) {
			if (sprint.getId().equals(sprintId)) {
				return sprint;
			}
		}
		return null;
	}

	public List<String> getStatuses() {
		return Collections.unmodifiableList(statuses);
	}

	public void setStatuses(List<String> statuses) {
		this.statuses.clear();
		this.statuses.addAll(statuses);
	}

	public List<String> getTicketTypes() {
		return Collections.unmodifiableList(ticketTypes);
	}

	public void setTicketTypes(List<String> ticketTypes) {
		this.ticketTypes.clear();
		this.ticketTypes.addAll(ticketTypes);
	}

	public int getNextTicketNumber() {
		return nextTicketNumber;
	}

	public int allocateTicketNumber() {
		return nextTicketNumber++;
	}

	public List<Ticket> getTickets() {
		return Collections.unmodifiableList(tickets);
	}

	public void addTicket(Ticket ticket) {
		tickets.add(ticket);
	}

	public Ticket findTicket(UUID ticketId) {
		for (Ticket ticket : tickets) {
			if (Objects.equals(ticket.getId(), ticketId)) {
				return ticket;
			}
		}
		return null;
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtUtil.putUuid(nbt, "id", id);
		nbt.putString("name", name);
		nbt.putString("description", description == null ? "" : description);
		NbtUtil.putUuid(nbt, "ownerId", ownerId);
		nbt.putString("ownerName", ownerName);
		nbt.putString("ticketPrefix", ticketPrefix);
		
		NbtList memberList = new NbtList();
		for (Member member : members.values()) {
			memberList.add(member.toNbt());
		}
		nbt.put("members", memberList);

		NbtList sprintList = new NbtList();
		for (Sprint sprint : sprints) {
			sprintList.add(sprint.toNbt());
		}
		nbt.put("sprints", sprintList);

		NbtList statusList = new NbtList();
		for (String status : statuses) {
			statusList.add(NbtString.of(status));
		}
		nbt.put("statuses", statusList);

		NbtList typeList = new NbtList();
		for (String type : ticketTypes) {
			typeList.add(NbtString.of(type));
		}
		nbt.put("ticketTypes", typeList);

		nbt.putInt("nextTicketNumber", nextTicketNumber);

		NbtList ticketList = new NbtList();
		for (Ticket ticket : tickets) {
			ticketList.add(ticket.toNbt());
		}
		nbt.put("tickets", ticketList);
		return nbt;
	}

	public static ProjectData fromNbt(NbtCompound nbt) {
		UUID id = NbtUtil.getUuid(nbt, "id");
		String name = nbt.getString("name", "");
		String description = nbt.getString("description", "");
		UUID ownerId = NbtUtil.getUuid(nbt, "ownerId");
		String ownerName = nbt.getString("ownerName", "");
		String ticketPrefix = nbt.getString("ticketPrefix", "PROJ");
		ProjectData data = new ProjectData(id, name, description, ownerId, ownerName, ticketPrefix);

		NbtList members = nbt.getListOrEmpty("members");
		for (int i = 0; i < members.size(); i++) {
			Member member = Member.fromNbt(members.getCompoundOrEmpty(i));
			data.members.put(member.getUuid(), member);
		}

		NbtList sprints = nbt.getListOrEmpty("sprints");
		for (int i = 0; i < sprints.size(); i++) {
			data.sprints.add(Sprint.fromNbt(sprints.getCompoundOrEmpty(i)));
		}

		NbtList statusList = nbt.getListOrEmpty("statuses");
		for (int i = 0; i < statusList.size(); i++) {
			data.statuses.add(statusList.getString(i, ""));
		}
		if (data.statuses.isEmpty()) {
			data.statuses.add("To Do");
			data.statuses.add("In Progress");
			data.statuses.add("Done");
			data.statuses.add("Blocked");
		}

		NbtList typeList = nbt.getListOrEmpty("ticketTypes");
		for (int i = 0; i < typeList.size(); i++) {
			data.ticketTypes.add(typeList.getString(i, ""));
		}
		if (data.ticketTypes.isEmpty()) {
			data.ticketTypes.add("Epic");
			data.ticketTypes.add("Story");
			data.ticketTypes.add("Task");
		}

		data.nextTicketNumber = nbt.getInt("nextTicketNumber", 1000);
		NbtList ticketList = nbt.getListOrEmpty("tickets");
		for (int i = 0; i < ticketList.size(); i++) {
			data.tickets.add(Ticket.fromNbt(ticketList.getCompoundOrEmpty(i)));
		}
		return data;
	}
}
