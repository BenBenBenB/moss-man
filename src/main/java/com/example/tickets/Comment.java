package com.example.tickets;

import java.util.UUID;
import com.example.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;

public class Comment {
	private final UUID id;
	private final UUID authorId;
	private final String authorName;
	private final String message;
	private final long createdAt;

	public Comment(UUID id, UUID authorId, String authorName, String message, long createdAt) {
		this.id = id;
		this.authorId = authorId;
		this.authorName = authorName;
		this.message = message;
		this.createdAt = createdAt;
	}

	public static Comment create(UUID authorId, String authorName, String message, long createdAt) {
		return new Comment(UUID.randomUUID(), authorId, authorName, message, createdAt);
	}

	public UUID getId() {
		return id;
	}

	public UUID getAuthorId() {
		return authorId;
	}

	public String getAuthorName() {
		return authorName;
	}

	public String getMessage() {
		return message;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtUtil.putUuid(nbt, "id", id);
		NbtUtil.putUuid(nbt, "authorId", authorId);
		nbt.putString("authorName", authorName);
		nbt.putString("message", message);
		nbt.putLong("createdAt", createdAt);
		return nbt;
	}

	public static Comment fromNbt(NbtCompound nbt) {
		UUID id = NbtUtil.getUuid(nbt, "id");
		UUID authorId = NbtUtil.getUuid(nbt, "authorId");
		String authorName = nbt.getString("authorName", "");
		String message = nbt.getString("message", "");
		long createdAt = nbt.getLong("createdAt", 0L);
		return new Comment(id, authorId, authorName, message, createdAt);
	}
}
