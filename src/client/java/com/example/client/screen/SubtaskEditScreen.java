package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.client.ui.MossuraUiState;
import com.example.project.ProjectData;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
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
		Ticket ticket = project.findTicket(ticketId);
		if (ticket == null) {
			return MossuraUiState.screenState("subtask-edit", project);
		}
		Subtask subtask = ticket.findSubtask(subtaskId);
		if (subtask == null) {
			return MossuraUiState.screenState("subtask-edit", project);
		}
		return MossuraUiState.subtaskState(project, ticket, subtask, "subtask-edit");
	}
}
