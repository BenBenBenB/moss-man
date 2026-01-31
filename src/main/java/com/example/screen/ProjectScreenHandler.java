package com.example.screen;

import com.example.network.ProjectViewTracker;
import com.example.registry.MossuraScreens;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

public class ProjectScreenHandler extends ScreenHandler {
	private final UUID projectId;
	private final NbtCompound initialProjectNbt;

	public ProjectScreenHandler(int syncId, PlayerInventory inventory, ProjectScreenOpenData data) {
		super(MossuraScreens.PROJECT_SCREEN_HANDLER, syncId);
		this.projectId = data.projectId();
		this.initialProjectNbt = data.projectNbt();
		if (inventory.player instanceof ServerPlayerEntity serverPlayer) {
			ProjectViewTracker.addViewer(this.projectId, serverPlayer);
		}
	}

	public UUID getProjectId() {
		return projectId;
	}

	public NbtCompound getInitialProjectNbt() {
		return initialProjectNbt;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (player instanceof ServerPlayerEntity serverPlayer) {
			ProjectViewTracker.removeViewer(projectId, serverPlayer);
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}
}
