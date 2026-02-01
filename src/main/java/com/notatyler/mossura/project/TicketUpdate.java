package com.notatyler.mossura.project;

import com.notatyler.mossura.tickets.TicketPriority;
import java.util.UUID;

public record TicketUpdate(
		String title,
		String description,
		TicketPriority priority,
		String type,
		String state,
		UUID assigneeId,
		String assigneeName,
		UUID sprintId,
		java.util.List<String> labels
) {
}
