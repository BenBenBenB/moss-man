package com.example.network;

import com.example.project.ProjectData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ProjectViewTracker {
	private static final Map<UUID, Set<UUID>> viewers = new HashMap<>();

	public static void addViewer(UUID projectId, ServerPlayerEntity player) {
		viewers.computeIfAbsent(projectId, ignored -> new HashSet<>()).add(player.getUuid());
	}

	public static void removeViewer(UUID projectId, ServerPlayerEntity player) {
		Set<UUID> projectViewers = viewers.get(projectId);
		if (projectViewers == null) {
			return;
		}
		projectViewers.remove(player.getUuid());
		if (projectViewers.isEmpty()) {
			viewers.remove(projectId);
		}
	}

	public static void syncToViewers(MinecraftServer server, ProjectData project) {
		Set<UUID> projectViewers = viewers.get(project.getId());
		if (projectViewers == null || projectViewers.isEmpty()) {
			return;
		}
		for (UUID playerId : new HashSet<>(projectViewers)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
			if (player != null) {
				MossuraNetwork.sendProjectSync(player, project);
			} else {
				projectViewers.remove(playerId);
			}
		}
	}
}
