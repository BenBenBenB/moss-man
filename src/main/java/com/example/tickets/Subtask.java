package com.example.tickets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.example.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class Subtask {
	private final UUID id;
	private final UUID creatorId;
	private final String creatorName;
	private String name;
	private String description;
	private final long createdAt;
	private boolean completed;
	private final List<Comment> comments = new ArrayList<>();

	public Subtask(UUID id, UUID creatorId, String creatorName, String name, String description, long createdAt) {
		this.id = id;
		this.creatorId = creatorId;
		this.creatorName = creatorName;
		this.name = name;
		this.description = description;
		this.createdAt = createdAt;
	}

	public static Subtask create(UUID creatorId, String creatorName, String name, String description, long createdAt) {
		return new Subtask(UUID.randomUUID(), creatorId, creatorName, name, description, createdAt);
	}

	public UUID getId() {
		return id;
	}

	public UUID getCreatorId() {
		return creatorId;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public List<Comment> getComments() {
		return Collections.unmodifiableList(comments);
	}

	public void addComment(Comment comment) {
		comments.add(comment);
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtUtil.putUuid(nbt, "id", id);
		NbtUtil.putUuid(nbt, "creatorId", creatorId);
		nbt.putString("creatorName", creatorName);
		nbt.putString("name", name);
		nbt.putString("description", description);
		nbt.putLong("createdAt", createdAt);
		if (completed) {
			nbt.putBoolean("completed", true);
		}
		NbtList commentList = new NbtList();
		for (Comment comment : comments) {
			commentList.add(comment.toNbt());
		}
		nbt.put("comments", commentList);
		return nbt;
	}

	public static Subtask fromNbt(NbtCompound nbt) {
		UUID id = NbtUtil.getUuid(nbt, "id");
		UUID creatorId = NbtUtil.getUuid(nbt, "creatorId");
		String creatorName = nbt.getString("creatorName", "");
		String name = nbt.getString("name", "");
		String description = nbt.getString("description", "");
		long createdAt = nbt.getLong("createdAt", 0L);
		Subtask subtask = new Subtask(id, creatorId, creatorName, name, description, createdAt);
		subtask.completed = nbt.getBoolean("completed", false);
		NbtList comments = nbt.getListOrEmpty("comments");
		for (int i = 0; i < comments.size(); i++) {
			subtask.comments.add(Comment.fromNbt(comments.getCompoundOrEmpty(i)));
		}
		return subtask;
	}
}
