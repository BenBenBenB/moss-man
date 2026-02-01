package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.project.ProjectData;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;

public class ProjectSettingsScreen extends MossuraMcefScreen {
	private final ProjectScreen parent;

	public ProjectSettingsScreen(ProjectScreen parent) {
		super(Text.literal("Project Settings"), "settings");
		this.parent = parent;
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return MossuraUiState.loadingState("settings");
		}
		return MossuraUiState.projectSettingsState(project, getViewerId(), getViewerName());
	}
}
