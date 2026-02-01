package com.notatyler.mossura.block;

import com.notatyler.mossura.block.entity.ProjectBlockEntity;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.project.ProjectStore;
import com.notatyler.mossura.screen.ProjectScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ProjectBlock extends Block implements BlockEntityProvider {
	public ProjectBlock(Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ProjectBlockEntity(pos, state);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (!world.isClient() && placer instanceof ServerPlayerEntity serverPlayer) {
			ensureProjectCreated((ServerWorld) world, pos, serverPlayer, itemStack);
		}
	}

	@Override
	public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof ProjectBlockEntity projectEntity && projectEntity.getProjectId() != null) {
			ProjectData project = ProjectStore.get(world.getServer()).getProject(projectEntity.getProjectId());
			if (project != null) {
				ItemStack stack = new ItemStack(this);
				net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
				nbt.put("project_data", project.toNbt());
				net.minecraft.component.type.NbtComponent.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, stack, nbt);
				dropStack(world, pos, stack);
			}
		}
		super.onStateReplaced(state, world, pos, moved);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.CONSUME;
		}
		ProjectData project = ensureProjectCreated((ServerWorld) world, pos, serverPlayer, player.getStackInHand(hit.getSide() == null ? net.minecraft.util.Hand.MAIN_HAND : (hit.getSide().getAxis() == net.minecraft.util.math.Direction.Axis.Y ? net.minecraft.util.Hand.MAIN_HAND : net.minecraft.util.Hand.MAIN_HAND))); // Simplified
		if (project == null) {
			serverPlayer.sendMessage(Text.literal("Project data unavailable"), false);
			return ActionResult.CONSUME;
		}
		serverPlayer.openHandledScreen(new ProjectScreenHandlerFactory(project));
		return ActionResult.CONSUME;
	}

	private ProjectData ensureProjectCreated(ServerWorld world, BlockPos pos, ServerPlayerEntity owner, ItemStack stack) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof ProjectBlockEntity projectEntity)) {
			return null;
		}
		ProjectStore store = ProjectStore.get(world.getServer());
		if (projectEntity.getProjectId() == null) {
			// Check if stack has project data
			net.minecraft.component.type.NbtComponent nbtComponent = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
			if (nbtComponent != null) {
				net.minecraft.nbt.NbtCompound projectNbt = nbtComponent.copyNbt().getCompound("project_data").orElse(new net.minecraft.nbt.NbtCompound());
				if (!projectNbt.isEmpty()) {
					ProjectData project = ProjectData.fromNbt(projectNbt);
					if (store.getProject(project.getId()) == null) {
						store.addProject(project);
					}
					projectEntity.setProjectId(project.getId());
					return project;
				}
			}

			String name = "Project @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			ProjectData project = ProjectData.create(owner.getUuid(), owner.getName().getString(), name, "PROJ");
			store.addProject(project);
			projectEntity.setProjectId(project.getId());
			projectEntity.markDirty();
			return project;
		}
		return store.getProject(projectEntity.getProjectId());
	}
}
