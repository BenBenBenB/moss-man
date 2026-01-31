package com.example.project;

import com.example.tickets.TicketPriority;
import java.util.UUID;

public record TicketUpdate(
		String title,
		String description,
		TicketPriority priority,
		String type,
		String state,
		UUID assigneeId,
		String assigneeName
) {
}
