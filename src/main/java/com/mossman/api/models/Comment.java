package com.mossman.api.models;

import java.util.UUID;

public record Comment(
    long id,
    long ticketId,
    UUID authorId,
    String authorName,
    String message,
    long createdAt
) {}
