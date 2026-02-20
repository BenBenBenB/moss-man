package com.mossman.domain.entities;

import java.util.UUID;

public enum Permission {
    NONE, VIEWER, CREATOR, EDITOR, ADMIN, OWNER;

    public static Permission fromString(String permission) {
        try {
            return Permission.valueOf(permission.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
