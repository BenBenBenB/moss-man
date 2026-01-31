package com.example.client.screen;

import com.example.client.state.ClientProjectCache;
import com.example.client.ui.MossuraUiState;
import com.example.project.ProjectData;
import com.example.tickets.Ticket;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.text.Text;

public class TicketViewerScreen extends MossuraMcefScreen {
	private final ProjectScreen parent;
	private final UUID ticketId;

	public TicketViewerScreen(ProjectScreen parent, UUID ticketId) {
		super(Text.literal("Ticket"), "ticket/" + ticketId);
		this.parent = parent;
		this.ticketId = ticketId;
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(parent.getProjectId());
		if (project == null) {
			return MossuraUiState.loadingState("ticket");
		}
		Ticket ticket = project.findTicket(ticketId);
		if (ticket == null) {
			return MossuraUiState.screenState("ticket", project);
		}
		return MossuraUiState.ticketState(project, ticket);
	}
}
