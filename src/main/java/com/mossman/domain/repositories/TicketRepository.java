package com.mossman.domain.repositories;

import com.mossman.domain.entities.Ticket;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(long id);
    List<Ticket> findByProjectId(long projectId);
    void delete(long id);
    int getNextTicketNumber(long projectId);
}
