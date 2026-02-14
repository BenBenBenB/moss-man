package com.mossman.api.models;

import java.util.UUID;
import com.mossman.api.enums.ProjectPermission;
import java.util.List;

public record Project(
    long id,
    String name,
    String description,
    String ticketPrefix,
    ProjectPermission externalUserPermission,
    String primaryTextColor,
    String iconTexture,
    List<Status> statuses,
    List<TicketType> ticketTypes,
    List<RelationshipType> relationshipTypes,
    List<Member> members
) {
    public record Status(
        String name,
        String foregroundColor,
        String backgroundColor
    ) {}

    public record TicketType(
        String name,
        String foregroundColor,
        String backgroundColor
    ) {}

    public record RelationshipType(
        String name,
        String sourceToTargetDescription,
        String targetToSourceDescription
    ) {}
}
