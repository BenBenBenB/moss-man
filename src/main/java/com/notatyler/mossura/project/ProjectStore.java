package com.notatyler.mossura.project;

import com.notatyler.mossura.Mossura;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

public class ProjectStore extends PersistentState {
	public static final String DATA_KEY = Mossura.MOD_ID + "_projects";
	private static final Codec<ProjectStore> CODEC = NbtCompound.CODEC.xmap(ProjectStore::fromNbt, ProjectStore::toNbt);
	public static final PersistentStateType<ProjectStore> TYPE = new PersistentStateType<>(DATA_KEY, ProjectStore::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	private final Map<UUID, ProjectData> projects = new HashMap<>();

	public static ProjectStore get(MinecraftServer server) {
		PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
		return manager.getOrCreate(TYPE);
	}

	public Collection<ProjectData> getProjects() {
		return projects.values();
	}

	public ProjectData getProject(UUID id) {
		return projects.get(id);
	}

	public void addProject(ProjectData project) {
		projects.put(project.getId(), project);
		markDirty();
	}

	public void markProjectDirty() {
		markDirty();
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtList projectList = new NbtList();
		for (ProjectData project : projects.values()) {
			projectList.add(project.toNbt());
		}
		nbt.put("projects", projectList);
		return nbt;
	}

	public static ProjectStore fromNbt(NbtCompound nbt) {
		ProjectStore store = new ProjectStore();
		NbtList projectList = nbt.getListOrEmpty("projects");
		for (int i = 0; i < projectList.size(); i++) {
			ProjectData project = ProjectData.fromNbt(projectList.getCompoundOrEmpty(i));
			store.projects.put(project.getId(), project);
		}
		return store;
	}
}
