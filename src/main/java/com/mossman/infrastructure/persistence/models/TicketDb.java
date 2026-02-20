package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Ticket;

import java.util.Collections;
import java.util.UUID;

@DatabaseTable(tableName = "tickets")
public class TicketDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private long projectId;

    @DatabaseField(canBeNull = false)
    private int ticketNumber;

    @DatabaseField(canBeNull = false)
    private String title;

    @DatabaseField
    private String description;

    @DatabaseField(canBeNull = false)
    private String type;

    @DatabaseField(canBeNull = false)
    private String status;

    @DatabaseField(canBeNull = false)
    private Priority priority;

    @DatabaseField(canBeNull = false)
    private UUID creator;

    @DatabaseField(canBeNull = false)
    private long createdAt;

    @DatabaseField(canBeNull = false)
    private long updatedAt;

    @DatabaseField
    private Long sprintId;

    public TicketDb() {}

    public TicketDb(Ticket ticket) {
        this.id = ticket.getId();
        this.projectId = ticket.getProjectId();
        this.ticketNumber = ticket.getTicketNumber();
        this.title = ticket.getTitle();
        this.description = ticket.getDescription();
        this.type = ticket.getType();
        this.status = ticket.getStatus();
        this.priority = ticket.getPriority();
        this.creator = ticket.getCreator();
        this.createdAt = ticket.getCreatedAt();
        this.updatedAt = ticket.getUpdatedAt();
        this.sprintId = ticket.getSprintId();
    }

    public Ticket toDomain() {
        return new Ticket(id, projectId, ticketNumber, title, description, type, status, priority, 
                Collections.emptyList(), Collections.emptyList(), creator, Collections.emptyList(), 
                createdAt, updatedAt, sprintId);
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }
    public int getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(int ticketNumber) { this.ticketNumber = ticketNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public UUID getCreator() { return creator; }
    public void setCreator(UUID creator) { this.creator = creator; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
}
