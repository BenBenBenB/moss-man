package com.notatyler.mossura.project;

import com.notatyler.mossura.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Sprint {
    public enum Status {
        PLANNED, ACTIVE, COMPLETED
    }

    private final UUID id;
    private String name;
    private long startTime;
    private long endTime;
    private Status status;
    private final List<UUID> ticketIds = new ArrayList<>();

    public Sprint(UUID id, String name, long startTime, long endTime, Status status) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public static Sprint create(String name, long startTime, long endTime) {
        return new Sprint(UUID.randomUUID(), name, startTime, endTime, Status.PLANNED);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<UUID> getTicketIds() { return ticketIds; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtUtil.putUuid(nbt, "id", id);
        nbt.putString("name", name);
        nbt.putLong("startTime", startTime);
        nbt.putLong("endTime", endTime);
        nbt.putString("status", status.name());
        NbtList list = new NbtList();
        for (UUID ticketId : ticketIds) {
            NbtCompound idNbt = new NbtCompound();
            NbtUtil.putUuid(idNbt, "id", ticketId);
            list.add(idNbt);
        }
        nbt.put("ticketIds", list);
        return nbt;
    }

    public static Sprint fromNbt(NbtCompound nbt) {
        UUID id = NbtUtil.getUuid(nbt, "id");
        String name = nbt.getString("name", "");
        long startTime = nbt.getLong("startTime", 0L);
        long endTime = nbt.getLong("endTime", 0L);
        Status status = Status.valueOf(nbt.getString("status", "PLANNED"));
        Sprint sprint = new Sprint(id, name, startTime, endTime, status);
        NbtList list = nbt.getListOrEmpty("ticketIds");
        for (int i = 0; i < list.size(); i++) {
            sprint.ticketIds.add(NbtUtil.getUuid(list.getCompoundOrEmpty(i), "id"));
        }
        return sprint;
    }
}
