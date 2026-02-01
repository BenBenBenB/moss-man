package com.notatyler.mossura.project;

import com.notatyler.mossura.util.NbtUtil;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public class Member {
    public enum PermissionLevel {
        VIEWER, EDITOR, ADMIN
    }

    private final UUID uuid;
    private String name;
    private PermissionLevel permissionLevel;

    public Member(UUID uuid, String name, PermissionLevel permissionLevel) {
        this.uuid = uuid;
        this.name = name;
        this.permissionLevel = permissionLevel;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtUtil.putUuid(nbt, "uuid", uuid);
        nbt.putString("name", name);
        nbt.putString("permission", permissionLevel.name());
        return nbt;
    }

    public static Member fromNbt(NbtCompound nbt) {
        UUID uuid = NbtUtil.getUuid(nbt, "uuid");
        String name = nbt.getString("name", "");
        PermissionLevel level = PermissionLevel.valueOf(nbt.getString("permission", "VIEWER"));
        return new Member(uuid, name, level);
    }
}
