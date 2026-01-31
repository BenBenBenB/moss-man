package com.example.project;

import com.example.tickets.Ticket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.example.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class ProjectData {
	private final UUID id;
	private String name;
	private final UUID ownerId;
	private String ownerName;
	private final Map<UUID, String> members = new LinkedHashMap<>();
	private final List<String> statuses = new ArrayList<>();
	private final List<String> ticketTypes = new ArrayList<>();
	private int nextTicketNumber = 1;
	private final List<Ticket> tickets = new ArrayList<>();

	public ProjectData(UUID id, String name, UUID ownerId, String ownerName) {
		this.id = id;
		this.name = name;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
	}

	public static ProjectData create(UUID ownerId, String ownerName, String name) {
		ProjectData data = new ProjectData(UUID.randomUUID(), name, ownerId, ownerName);
		data.statuses.add("To Do");
		data.statuses.add("In Progress");
		data.statuses.add("Done");
		data.ticketTypes.add("Epic");
		data.ticketTypes.add("Story");
		data.ticketTypes.add("Task");
		data.ticketTypes.add("Bug");
		return data;
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

	public Map<UUID, String> getMembers() {
		return Collections.unmodifiableMap(members);
	}

	public void addMember(UUID memberId, String memberName) {
		members.put(memberId, memberName);
	}

	public void removeMember(UUID memberId) {
		members.remove(memberId);
	}

	public UUID findMemberIdByName(String name) {
		for (Map.Entry<UUID, String> entry : members.entrySet()) {
			if (entry.getValue().equalsIgnoreCase(name)) {
				return entry.getKey();
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
		NbtUtil.putUuid(nbt, "ownerId", ownerId);
		nbt.putString("ownerName", ownerName);
		NbtList memberList = new NbtList();
		for (Map.Entry<UUID, String> entry : members.entrySet()) {
			NbtCompound memberNbt = new NbtCompound();
			NbtUtil.putUuid(memberNbt, "id", entry.getKey());
			memberNbt.putString("name", entry.getValue());
			memberList.add(memberNbt);
		}
		nbt.put("members", memberList);
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
		UUID ownerId = NbtUtil.getUuid(nbt, "ownerId");
		String ownerName = nbt.getString("ownerName", "");
		ProjectData data = new ProjectData(id, name, ownerId, ownerName);
		NbtList members = nbt.getListOrEmpty("members");
		for (int i = 0; i < members.size(); i++) {
			NbtCompound memberNbt = members.getCompoundOrEmpty(i);
			UUID memberId = NbtUtil.getUuid(memberNbt, "id");
			String memberName = memberNbt.getString("name", "");
			if (memberId != null) {
				data.members.put(memberId, memberName);
			}
		}
		NbtList statusList = nbt.getListOrEmpty("statuses");
		for (int i = 0; i < statusList.size(); i++) {
			data.statuses.add(statusList.getString(i, ""));
		}
		if (data.statuses.isEmpty()) {
			data.statuses.add("To Do");
			data.statuses.add("In Progress");
			data.statuses.add("Done");
		}
		NbtList typeList = nbt.getListOrEmpty("ticketTypes");
		for (int i = 0; i < typeList.size(); i++) {
			data.ticketTypes.add(typeList.getString(i, ""));
		}
		if (data.ticketTypes.isEmpty()) {
			data.ticketTypes.add("Epic");
			data.ticketTypes.add("Story");
			data.ticketTypes.add("Task");
			data.ticketTypes.add("Bug");
		}
		data.nextTicketNumber = nbt.getInt("nextTicketNumber", 1);
		NbtList ticketList = nbt.getListOrEmpty("tickets");
		for (int i = 0; i < ticketList.size(); i++) {
			data.tickets.add(Ticket.fromNbt(ticketList.getCompoundOrEmpty(i)));
		}
		return data;
	}
}
