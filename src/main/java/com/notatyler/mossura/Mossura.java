package com.notatyler.mossura;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.notatyler.mossura.network.MossuraNetwork;
import com.notatyler.mossura.network.MossuraPayloads;
import com.notatyler.mossura.notification.SessionEvents;
import com.notatyler.mossura.registry.MossuraBlocks;
import com.notatyler.mossura.registry.MossuraItems;
import com.notatyler.mossura.registry.MossuraScreens;
import com.notatyler.mossura.registry.MossuraVillagers;

public class Mossura implements ModInitializer {
	public static final String MOD_ID = "mossura";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static com.notatyler.mossura.web.MossuraWebServer webServer;

	@Override
	public void onInitialize() {
		MossuraPayloads.register();
		MossuraBlocks.register();
		MossuraItems.register();
		MossuraScreens.register();
		MossuraVillagers.register();
		MossuraNetwork.registerServerReceivers();
		com.notatyler.mossura.notification.SessionEvents.register();
		
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			webServer = new com.notatyler.mossura.web.MossuraWebServer(server);
			webServer.start();
		});

		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(net.minecraft.server.command.CommandManager.literal("mossura")
				.then(net.minecraft.server.command.CommandManager.literal("verify")
					.then(net.minecraft.server.command.CommandManager.argument("token", com.mojang.brigadier.arguments.StringArgumentType.string())
						.executes(context -> {
							String token = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "token");
							net.minecraft.server.network.ServerPlayerEntity player = context.getSource().getPlayer();
							if (player == null) return 0;
							
							if (com.notatyler.mossura.web.MossuraAuthManager.verify(token, player.getUuid())) {
								context.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§aSuccessfully verified web session!"), false);
								return 1;
							} else {
								context.getSource().sendError(net.minecraft.text.Text.literal("Invalid or expired token."));
								return 0;
							}
						})
					)
				)
			);
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (webServer != null) {
				webServer.stop();
			}
		});

		LOGGER.info("Mossura initialized");
	}
}
