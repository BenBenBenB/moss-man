package com.mossman.domain.usecases;

import com.mossman.domain.entities.Ticket;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.TicketCreatedEvent;
import com.mossman.domain.repositories.TicketRepository;
import java.time.Instant;

public class CreateTicketUseCase {
    private final TicketRepository ticketRepository;
    private final DomainEventBus eventBus;

    public CreateTicketUseCase(TicketRepository ticketRepository, DomainEventBus eventBus) {
        this.ticketRepository = ticketRepository;
        this.eventBus = eventBus;
    }

    public Ticket execute(Ticket ticket) {
        Ticket savedTicket = ticketRepository.save(ticket);
        eventBus.publish(new TicketCreatedEvent(savedTicket, Instant.now()));
        return savedTicket;
    }
}
