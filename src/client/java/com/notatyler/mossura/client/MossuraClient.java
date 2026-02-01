package com.notatyler.mossura.client;

import com.cinemamod.mcef.MCEF;
import com.notatyler.mossura.client.screen.ProjectScreen;
import com.notatyler.mossura.client.state.ClientProjectCache;
import com.notatyler.mossura.client.ui.MossuraScheme;
import com.notatyler.mossura.client.ui.MossuraUiBridge;
import com.notatyler.mossura.client.ui.MossuraUiRefreshable;
import com.notatyler.mossura.network.MossuraPayloads;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.registry.MossuraScreens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MossuraClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		net.minecraft.client.gui.screen.ingame.HandledScreens.register(MossuraScreens.PROJECT_SCREEN_HANDLER, ProjectScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(MossuraPayloads.ProjectSyncPayload.ID, (payload, context) -> {
			ProjectData project = ProjectData.fromNbt(payload.projectNbt());
			context.client().execute(() -> {
				ClientProjectCache.put(project);
				if (context.client().currentScreen instanceof ProjectScreen projectScreen && projectScreen.getProjectId().equals(payload.projectId())) {
					projectScreen.applyProjectSync(project);
				} else if (context.client().currentScreen instanceof MossuraUiRefreshable refreshable) {
					refreshable.refreshUiState();
				}
			});
		});
		registerMcefScheme();
		MossuraUiBridge.register();
	}

	private static void registerMcefScheme() {
		Runnable register = () -> {
			try {
				MCEF.getApp().getHandle().registerSchemeHandlerFactory(
						"mossura",
						"",
						(browser, frame, url, request) -> new MossuraScheme(request.getURL())
				);
			} catch (Exception e) {
				MCEF.getLogger().error("Failed to register mossura scheme handler", e);
			}
		};

		if (MCEF.isInitialized()) {
			register.run();
		} else {
			MCEF.scheduleForInit(success -> {
				if (success) {
					register.run();
				} else {
					MCEF.getLogger().warn("MCEF did not initialize; mossura scheme not registered.");
				}
			});
		}
	}
}
