package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.client.ui.MossuraUiState;
import com.example.project.ProjectData;
import com.example.tickets.Ticket;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class TicketCommentScreen extends MossuraMcefScreen {
	private final UUID projectId;
	private final UUID ticketId;

	public TicketCommentScreen(Screen parent, UUID projectId, UUID ticketId) {
		super(Text.literal("Add Comment"), "ticket/" + ticketId + "/comment");
		this.projectId = projectId;
		this.ticketId = ticketId;
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(projectId);
		if (project == null) {
			return MossuraUiState.loadingState("ticket-comment");
		}
		Ticket ticket = project.findTicket(ticketId);
		if (ticket == null) {
			return MossuraUiState.screenState("ticket-comment", project);
		}
		return MossuraUiState.ticketCommentState(project, ticket);
	}
}
