package com.notatyler.mossura.client.screen;

import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.tickets.Ticket;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.text.Text;

public class TicketEditorScreen extends MossuraMcefScreen {
	private final UUID projectId;
	private final UUID ticketId;

	private TicketEditorScreen(ProjectScreen parent, UUID projectId, UUID ticketId) {
		super(Text.literal(ticketId == null ? "Create Ticket" : "Edit Ticket"),
				ticketId == null ? "ticket/new" : "ticket/" + ticketId + "/edit");
		this.projectId = projectId;
		this.ticketId = ticketId;
	}

	public static TicketEditorScreen createNew(ProjectScreen parent, UUID projectId) {
		return new TicketEditorScreen(parent, projectId, null);
	}

	public static TicketEditorScreen editExisting(ProjectScreen parent, UUID ticketId) {
		return new TicketEditorScreen(parent, parent.getProjectId(), ticketId);
	}

	@Override
	protected JsonObject buildState() {
		ProjectData project = ClientProjectCache.get(projectId);
		if (project == null) {
			return MossuraUiState.loadingState(ticketId == null ? "ticket-new" : "ticket-edit");
		}
		Ticket ticket = ticketId == null ? null : project.findTicket(ticketId);
		return MossuraUiState.ticketEditorState(project, ticket, getViewerId(), getViewerName());
	}
}
