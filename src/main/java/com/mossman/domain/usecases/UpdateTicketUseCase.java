package com.mossman.domain.usecases;

import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.repositories.TicketRepository;

import java.util.Map;

/**
 * Applies a partial update (patch) to a ticket using a String->String map of field values.
 * Only fields present in the patch map are changed; all others are left as-is.
 * The caller (command layer) is responsible for parsing SNBT into the map.
 *
 * Patchable fields: title, description, status, type, priority
 */
public class UpdateTicketUseCase {
    private final TicketRepository ticketRepository;
    private final DomainEventBus eventBus;

    public UpdateTicketUseCase(TicketRepository ticketRepository, DomainEventBus eventBus) {
        this.ticketRepository = ticketRepository;
        this.eventBus = eventBus;
    }

    /**
     * @param ticketId DB id of the ticket to patch
     * @param patch    map of field name → new string value (from SNBT)
     * @return the updated Ticket
     * @throws IllegalArgumentException if the ticket does not exist or a field value is invalid
     */
    public Ticket execute(long ticketId, Map<String, String> patch) {
        Ticket current = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: id=" + ticketId));

        String title       = patch.getOrDefault("title",       current.getTitle());
        String description = patch.getOrDefault("description", current.getDescription() != null ? current.getDescription() : "");
        String type        = patch.getOrDefault("type",        current.getType());
        String status      = patch.getOrDefault("status",      current.getStatus()).toUpperCase();
        Priority priority  = patch.containsKey("priority")
                ? Priority.valueOf(patch.get("priority").toUpperCase())
                : current.getPriority();

        Ticket updated = new Ticket(
                current.getId(),
                current.getProjectId(),
                current.getTicketNumber(),
                title,
                description,
                type,
                status,
                priority,
                current.getAssignees(),
                current.getObservers(),
                current.getCreator(),
                current.getLabels(),
                current.getCreatedAt(),
                System.currentTimeMillis(),
                current.getSprintId()
        );

        return ticketRepository.save(updated);
    }
}
