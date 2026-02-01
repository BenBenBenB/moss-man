package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.tickets.Subtask;
import com.notatyler.mossura.tickets.Ticket;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class SubtaskCommentScreen extends MossuraMcefScreen {
	private final UUID projectId;
	private final UUID ticketId;
	private final UUID subtaskId;

	public SubtaskCommentScreen(Screen parent, UUID projectId, UUID ticketId, UUID subtaskId) {
		super(Text.literal("Subtask Comment"), "ticket/" + ticketId + "/subtask/" + subtaskId + "/comment");
		this.projectId = projectId;
		this.ticketId = ticketId;
		this.subtaskId = subtaskId;
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(projectId);
		if (project == null) {
			return MossuraUiState.loadingState("subtask-comment");
		}
		long nowMillis = System.currentTimeMillis();
		long worldTicks = getWorldTicks();
		Ticket ticket = project.findTicket(ticketId);
		if (ticket == null) {
			return MossuraUiState.screenState("subtask-comment", project, getViewerId(), getViewerName(), nowMillis, worldTicks);
		}
		Subtask subtask = ticket.findSubtask(subtaskId);
		if (subtask == null) {
			return MossuraUiState.screenState("subtask-comment", project, getViewerId(), getViewerName(), nowMillis, worldTicks);
		}
		return MossuraUiState.subtaskState(project, ticket, subtask, "subtask-comment", getViewerId(), getViewerName(), nowMillis, worldTicks);
	}
}
