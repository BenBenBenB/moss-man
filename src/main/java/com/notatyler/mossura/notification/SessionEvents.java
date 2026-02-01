package com.notatyler.mossura.notification;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionEvents {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerUuid = player.getUuid();
            
            scheduler.schedule(() -> {
                server.execute(() -> {
                    ServerPlayerEntity currentPlayer = server.getPlayerManager().getPlayer(playerUuid);
                    if (currentPlayer != null && currentPlayer.networkHandler.isConnectionOpen()) {
                        NotificationManager manager = NotificationManager.get(server);
                        List<String> notifications = manager.getAndClearNotifications(playerUuid);
                        
                        if (!notifications.isEmpty()) {
                            currentPlayer.sendMessage(Text.literal("--- Mossura Updates ---").formatted(Formatting.GOLD), false);
                            for (String msg : notifications) {
                                currentPlayer.sendMessage(Text.literal(msg).formatted(Formatting.GRAY), false);
                            }
                        }
                    }
                });
            }, 10, TimeUnit.SECONDS);
        });
    }
}
