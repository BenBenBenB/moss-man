package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mossman.infrastructure.persistence.models.ProjectDb;
import com.mossman.infrastructure.persistence.models.TicketDb;

import java.sql.SQLException;

public class DatabaseManager {
    private final ConnectionSource connectionSource;
    private final Dao<ProjectDb, Long> projectDao;
    private final Dao<TicketDb, Long> ticketDao;

    public DatabaseManager(String databaseUrl) throws SQLException {
        this.connectionSource = new JdbcConnectionSource(databaseUrl);
        
        // Create tables if they don't exist
        TableUtils.createTableIfNotExists(connectionSource, ProjectDb.class);
        TableUtils.createTableIfNotExists(connectionSource, TicketDb.class);
        
        this.projectDao = DaoManager.createDao(connectionSource, ProjectDb.class);
        this.ticketDao = DaoManager.createDao(connectionSource, TicketDb.class);
    }

    public Dao<ProjectDb, Long> getProjectDao() { return projectDao; }
    public Dao<TicketDb, Long> getTicketDao() { return ticketDao; }

    public void close() throws Exception {
        connectionSource.close();
    }
}
