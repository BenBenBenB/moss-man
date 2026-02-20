package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.repositories.TicketRepository;
import com.mossman.infrastructure.persistence.models.TicketDb;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrmLiteTicketRepository implements TicketRepository {
    private final Dao<TicketDb, Long> ticketDao;

    public OrmLiteTicketRepository(Dao<TicketDb, Long> ticketDao) {
        this.ticketDao = ticketDao;
    }

    @Override
    public Ticket save(Ticket ticket) {
        try {
            TicketDb dbModel = new TicketDb(ticket);
            ticketDao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save ticket", e);
        }
    }

    @Override
    public Optional<Ticket> findById(long id) {
        try {
            TicketDb dbModel = ticketDao.queryForId(id);
            return Optional.ofNullable(dbModel).map(TicketDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ticket by id", e);
        }
    }

    @Override
    public List<Ticket> findByProjectId(long projectId) {
        try {
            return ticketDao.queryForEq("projectId", projectId).stream()
                    .map(TicketDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find tickets by projectId", e);
        }
    }

    @Override
    public void delete(long id) {
        try {
            ticketDao.deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete ticket", e);
        }
    }

    @Override
    public int getNextTicketNumber(long projectId) {
        try {
            // Very simple implementation: count + 1. 
            // In a real system, we'd want a separate counter table or a max() query.
            long count = ticketDao.countOf(ticketDao.queryBuilder().where().eq("projectId", projectId).prepare());
            return (int) (count + 1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get next ticket number", e);
        }
    }
}
