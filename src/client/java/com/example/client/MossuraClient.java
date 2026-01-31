package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.example.client.screen.ProjectScreen;
import com.example.client.state.ClientProjectCache;
import com.example.network.MossuraPayloads;
import com.example.project.ProjectData;
import com.example.registry.MossuraScreens;

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
				}
			});
		});
	}
}
