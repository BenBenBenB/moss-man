package com.mossman.domain.entities;

import java.util.UUID;

public record Member(long id, UUID uuid, String name, Permission permission) {
}
