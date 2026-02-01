package com.notatyler.mossura.screen;

import com.notatyler.mossura.project.ProjectData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class ProjectScreenHandlerFactory implements ExtendedScreenHandlerFactory<ProjectScreenOpenData> {
	private final ProjectData project;

	public ProjectScreenHandlerFactory(ProjectData project) {
		this.project = project;
	}

	@Override
	public ProjectScreenOpenData getScreenOpeningData(ServerPlayerEntity player) {
		return new ProjectScreenOpenData(project.getId(), project.toNbt());
	}

	@Override
	public Text getDisplayName() {
		return Text.literal(project.getName());
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
		return new ProjectScreenHandler(syncId, inventory, new ProjectScreenOpenData(project.getId(), project.toNbt()));
	}
}
