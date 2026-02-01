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

public class SubtaskEditScreen extends MossuraMcefScreen {
	private final UUID projectId;
	private final UUID ticketId;
	private final UUID subtaskId;

	public SubtaskEditScreen(Screen parent, UUID projectId, UUID ticketId, Subtask subtask) {
		super(Text.literal("Edit Subtask"), "ticket/" + ticketId + "/subtask/" + subtask.getId() + "/edit");
		this.projectId = projectId;
		this.ticketId = ticketId;
		this.subtaskId = subtask.getId();
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(projectId);
		if (project == null) {
			return MossuraUiState.loadingState("subtask-edit");
		}
		long nowMillis = System.currentTimeMillis();
		long worldTicks = getWorldTicks();
		Ticket ticket = project.findTicket(ticketId);
		if (ticket == null) {
			return MossuraUiState.screenState("subtask-edit", project, getViewerId(), getViewerName(), nowMillis, worldTicks);
		}
		Subtask subtask = ticket.findSubtask(subtaskId);
		if (subtask == null) {
			return MossuraUiState.screenState("subtask-edit", project, getViewerId(), getViewerName(), nowMillis, worldTicks);
		}
		return MossuraUiState.subtaskState(project, ticket, subtask, "subtask-edit", getViewerId(), getViewerName(), nowMillis, worldTicks);
	}
}
