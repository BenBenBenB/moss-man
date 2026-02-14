package com.mossman.api.models;

import com.mossman.api.enums.Priority;
import java.util.List;
import java.util.UUID;

public record Ticket(
    long id,
    long projectId,
    int ticketNumber,
    String title,
    String description,
    String type,
    String status,
    Priority priority,
    List<UUID> assignees,
    List<UUID> observers,
    UUID reporter,
    List<String> labels,
    long createdAt,
    long updatedAt,
    Long sprintId
) {
    public String getFormattedKey(String prefix) {
        return prefix + "-" + ticketNumber;
    }
}
