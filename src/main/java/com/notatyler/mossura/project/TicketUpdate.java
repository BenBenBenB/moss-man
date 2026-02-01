package com.notatyler.mossura.project;

import com.notatyler.mossura.tickets.TicketPriority;
import java.util.UUID;

public record TicketUpdate(
		String title,
		String description,
		TicketPriority priority,
		String type,
		String state,
		java.util.List<UUID> assigneeIds,
		java.util.List<String> assigneeNames,
		UUID sprintId,
		java.util.List<String> labels
) {
}
