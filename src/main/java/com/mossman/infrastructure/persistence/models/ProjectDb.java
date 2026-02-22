package com.mossman.infrastructure.persistence.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;

import java.util.UUID;

@DatabaseTable(tableName = "projects")
public class ProjectDb {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false, unique = true)
    private String ticketPrefix;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField
    private String description;

    @DatabaseField
    private String iconTexture;

    @DatabaseField(canBeNull = false)
    private Permission externalUserPermission;

    @DatabaseField
    private String textColor;

    // OrmLite requires a no-arg constructor
    public ProjectDb() {}

    public ProjectDb(Project project) {
        this.id = project.getId();
        this.ticketPrefix = project.getTicketPrefix();
        this.name = project.getName();
        this.description = project.getDescription();
        // owner removed; will be handled via members table
        this.iconTexture = project.getIconTexture();
        this.externalUserPermission = project.getExternalUserPermission();
        this.textColor = project.getTextColor();
    }

    public Project toDomain() {
        return toDomain(java.util.Collections.emptyList());
    }

    public Project toDomain(java.util.List<com.mossman.domain.entities.Member> members) {
        return Project.builder()
                .id(id)
                .ticketPrefix(ticketPrefix)
                .name(name)
                .description(description)
                .iconTexture(iconTexture)
                .externalUserPermission(externalUserPermission)
                .textColor(textColor)
                .members(members)
                .build();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTicketPrefix() { return ticketPrefix; }
    public void setTicketPrefix(String ticketPrefix) { this.ticketPrefix = ticketPrefix; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconTexture() { return iconTexture; }
    public void setIconTexture(String iconTexture) { this.iconTexture = iconTexture; }
    public Permission getExternalUserPermission() { return externalUserPermission; }
    public void setExternalUserPermission(Permission externalUserPermission) { this.externalUserPermission = externalUserPermission; }
    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
}
