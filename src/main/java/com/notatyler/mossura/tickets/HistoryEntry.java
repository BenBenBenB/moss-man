package com.notatyler.mossura.tickets;

import java.util.UUID;
import com.notatyler.mossura.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;

public class HistoryEntry {
	private final UUID id;
	private final UUID actorId;
	private final String actorName;
	private final String action;
	private final String field;
	private final String beforeValue;
	private final String afterValue;
	private final String targetType;
	private final UUID targetId;
	private final long timestamp;

	public HistoryEntry(UUID id, UUID actorId, String actorName, String action, String field, String beforeValue, String afterValue, String targetType, UUID targetId, long timestamp) {
		this.id = id;
		this.actorId = actorId;
		this.actorName = actorName;
		this.action = action;
		this.field = field;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
		this.targetType = targetType;
		this.targetId = targetId;
		this.timestamp = timestamp;
	}

	public static HistoryEntry create(UUID actorId, String actorName, String action, String field, String beforeValue, String afterValue, String targetType, UUID targetId, long timestamp) {
		return new HistoryEntry(UUID.randomUUID(), actorId, actorName, action, field, beforeValue, afterValue, targetType, targetId, timestamp);
	}

	public UUID getId() {
		return id;
	}

	public UUID getActorId() {
		return actorId;
	}

	public String getActorName() {
		return actorName;
	}

	public String getAction() {
		return action;
	}

	public String getField() {
		return field;
	}

	public String getBeforeValue() {
		return beforeValue;
	}

	public String getAfterValue() {
		return afterValue;
	}

	public String getTargetType() {
		return targetType;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtUtil.putUuid(nbt, "id", id);
		NbtUtil.putUuid(nbt, "actorId", actorId);
		nbt.putString("actorName", actorName);
		nbt.putString("action", action);
		nbt.putString("field", field == null ? "" : field);
		nbt.putString("beforeValue", beforeValue == null ? "" : beforeValue);
		nbt.putString("afterValue", afterValue == null ? "" : afterValue);
		nbt.putString("targetType", targetType == null ? "" : targetType);
		if (targetId != null) {
			NbtUtil.putUuid(nbt, "targetId", targetId);
		}
		nbt.putLong("timestamp", timestamp);
		return nbt;
	}

	public static HistoryEntry fromNbt(NbtCompound nbt) {
		UUID id = NbtUtil.getUuid(nbt, "id");
		UUID actorId = NbtUtil.getUuid(nbt, "actorId");
		String actorName = nbt.getString("actorName", "");
		String action = nbt.getString("action", "");
		String field = nbt.getString("field", "");
		String beforeValue = nbt.getString("beforeValue", "");
		String afterValue = nbt.getString("afterValue", "");
		String targetType = nbt.getString("targetType", "");
		UUID targetId = NbtUtil.getUuid(nbt, "targetId");
		long timestamp = nbt.getLong("timestamp", 0L);
		return new HistoryEntry(id, actorId, actorName, action, field, beforeValue, afterValue, targetType, targetId, timestamp);
	}
}
