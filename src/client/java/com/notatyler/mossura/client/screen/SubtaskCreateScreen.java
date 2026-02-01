package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.tickets.Ticket;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class SubtaskCreateScreen extends MossuraMcefScreen {
	private final UUID projectId;
	private final UUID ticketId;

	public SubtaskCreateScreen(Screen parent, UUID projectId, UUID ticketId) {
		super(Text.literal("New Subtask"), "ticket/" + ticketId + "/subtask/new");
		this.projectId = projectId;
		this.ticketId = ticketId;
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(projectId);
		if (project == null) {
			return MossuraUiState.loadingState("subtask-new");
		}
		Ticket ticket = project.findTicket(ticketId);
		long nowMillis = System.currentTimeMillis();
		long worldTicks = getWorldTicks();
		if (ticket == null) {
			return MossuraUiState.screenState("subtask-new", project, getViewerId(), getViewerName(), nowMillis, worldTicks);
		}
		return MossuraUiState.subtaskState(project, ticket, null, "subtask-new", getViewerId(), getViewerName(), nowMillis, worldTicks);
	}
}
