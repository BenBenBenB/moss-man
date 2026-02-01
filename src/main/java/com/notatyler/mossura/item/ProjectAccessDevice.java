package com.notatyler.mossura.item;

import com.notatyler.mossura.block.entity.ProjectBlockEntity;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.project.ProjectStore;
import com.notatyler.mossura.screen.ProjectScreenHandlerFactory;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ProjectAccessDevice extends Item {
	public ProjectAccessDevice(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
			return ActionResult.CONSUME;
		}
		if (world.getBlockEntity(context.getBlockPos()) instanceof ProjectBlockEntity projectEntity) {
			UUID projectId = projectEntity.getProjectId();
			if (projectId != null) {
				NbtComponent.set(DataComponentTypes.CUSTOM_DATA, context.getStack(), nbt -> nbt.putString("projectId", projectId.toString()));
				player.sendMessage(Text.literal("Linked wireless access to project."), false);
				return ActionResult.CONSUME;
			}
		}
		player.sendMessage(Text.literal("This isn't a project block."), false);
		return ActionResult.CONSUME;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(user instanceof ServerPlayerEntity player)) {
			return ActionResult.PASS;
		}
		NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		String projectIdValue = data.copyNbt().getString("projectId", "");
		if (projectIdValue.isEmpty()) {
			player.sendMessage(Text.literal("Wireless access is not linked to a project."), false);
			return ActionResult.CONSUME;
		}
		UUID projectId = UUID.fromString(projectIdValue);
		ProjectStore store = ProjectStore.get(((ServerWorld) world).getServer());
		ProjectData project = store.getProject(projectId);
		if (project == null) {
			player.sendMessage(Text.literal("Linked project not found."), false);
			return ActionResult.CONSUME;
		}
		player.openHandledScreen(new ProjectScreenHandlerFactory(project));
		return ActionResult.CONSUME;
	}
}
