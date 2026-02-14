package com.mossman.api.models;

import java.util.UUID;
import com.mossman.api.enums.ProjectPermission;

public record Member(
    long id,
    UUID uuid,
    String name,
    ProjectPermission permission
) {}
