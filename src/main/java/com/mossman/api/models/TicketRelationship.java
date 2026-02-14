package com.mossman.api.models;

public record TicketRelationship(
    long id,
    long sourceTicketId,
    long targetTicketId,
    String type
) {}
