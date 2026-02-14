package com.mossman.api.models;

import com.mossman.api.enums.SprintStatus;
import java.util.UUID;

public record Sprint(
    long id,
    long projectId,
    String name,
    long startTime,
    long endTime,
    SprintStatus status
) {}
