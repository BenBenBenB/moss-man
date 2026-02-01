package com.notatyler.mossura.notification;

import com.notatyler.mossura.Mossura;
import com.notatyler.mossura.util.NbtUtil;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public class NotificationManager extends PersistentState {
    private static final String DATA_KEY = Mossura.MOD_ID + "_notifications";
    
    private static class PendingNotification {
        final UUID ticketId;
        final String message;

        PendingNotification(UUID ticketId, String message) {
            this.ticketId = ticketId;
            this.message = message;
        }
    }

    private final Map<UUID, List<PendingNotification>> pendingNotifications = new HashMap<>();

    public static final PersistentStateType<NotificationManager> TYPE = new PersistentStateType<>(
            DATA_KEY,
            NotificationManager::new,
            NbtCompound.CODEC.xmap(NotificationManager::fromNbt, NotificationManager::toNbt),
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public static NotificationManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }

    public void addAssignmentNotification(UUID playerUuid, UUID ticketId, String message) {
        // Remove existing notification for the same ticket if it exists
        cancelAssignmentNotification(playerUuid, ticketId);
        pendingNotifications.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(new PendingNotification(ticketId, message));
        markDirty();
    }

    public void cancelAssignmentNotification(UUID playerUuid, UUID ticketId) {
        List<PendingNotification> notifications = pendingNotifications.get(playerUuid);
        if (notifications != null) {
            if (notifications.removeIf(n -> Objects.equals(n.ticketId, ticketId))) {
                markDirty();
            }
        }
    }

    public List<String> getAndClearNotifications(UUID playerUuid) {
        List<PendingNotification> notifications = pendingNotifications.remove(playerUuid);
        if (notifications != null) {
            markDirty();
            return notifications.stream().map(n -> n.message).toList();
        }
        return Collections.emptyList();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        for (Map.Entry<UUID, List<PendingNotification>> entry : pendingNotifications.entrySet()) {
            NbtCompound entryNbt = new NbtCompound();
            NbtUtil.putUuid(entryNbt, "uuid", entry.getKey());
            NbtList msgs = new NbtList();
            for (PendingNotification n : entry.getValue()) {
                NbtCompound msgNbt = new NbtCompound();
                if (n.ticketId != null) NbtUtil.putUuid(msgNbt, "ticketId", n.ticketId);
                msgNbt.putString("message", n.message);
                msgs.add(msgNbt);
            }
            entryNbt.put("messages", msgs);
            list.add(entryNbt);
        }
        nbt.put("pending", list);
        return nbt;
    }

    public static NotificationManager fromNbt(NbtCompound nbt) {
        NotificationManager manager = new NotificationManager();
        NbtList list = nbt.getListOrEmpty("pending");
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entryNbt = list.getCompoundOrEmpty(i);
            UUID uuid = NbtUtil.getUuid(entryNbt, "uuid");
            NbtList msgsIn = entryNbt.getListOrEmpty("messages");
            List<PendingNotification> msgs = new ArrayList<>();
            for (int j = 0; j < msgsIn.size(); j++) {
                NbtCompound msgNbt = msgsIn.getCompoundOrEmpty(j);
                msgs.add(new PendingNotification(NbtUtil.getUuid(msgNbt, "ticketId"), msgNbt.getString("message", "")));
            }
            if (uuid != null) {
                manager.pendingNotifications.put(uuid, msgs);
            }
        }
        return manager;
    }
}
