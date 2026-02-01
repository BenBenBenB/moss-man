package com.notatyler.mossura.client.state;

import com.notatyler.mossura.project.ProjectData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientProjectCache {
	private static final Map<UUID, ProjectData> projects = new HashMap<>();

	public static void put(ProjectData project) {
		projects.put(project.getId(), project);
	}

	public static ProjectData get(UUID projectId) {
		return projects.get(projectId);
	}
}
