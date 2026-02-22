package com.mossman.domain.usecases;

import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketCreatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.domain.entities.Permission;
import java.time.Instant;

public class CreateTicketUseCase {
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public CreateTicketUseCase(TicketRepository ticketRepository, ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    public Ticket execute(Ticket ticket, java.util.UUID requesterId) {
        com.mossman.domain.entities.Project project = projectRepository.findById(ticket.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: id=" + ticket.getProjectId()));

        Permission perm = com.mossman.domain.auth.PermissionChecker.getEffectivePermission(project, requesterId);
        if (perm == Permission.FORBID || perm == Permission.VIEWER) {
            throw new SecurityException("Insufficient permission: CREATOR required");
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        eventBus.publish(new TicketCreatedEvent(savedTicket, Instant.now()));
        return savedTicket;
    }
}
