package com.example.block;

import com.example.block.entity.ProjectBlockEntity;
import com.example.project.ProjectData;
import com.example.project.ProjectStore;
import com.example.screen.ProjectScreenHandlerFactory;
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
			ensureProjectCreated((ServerWorld) world, pos, serverPlayer);
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.CONSUME;
		}
		ProjectData project = ensureProjectCreated((ServerWorld) world, pos, serverPlayer);
		if (project == null) {
			serverPlayer.sendMessage(Text.literal("Project data unavailable"), false);
			return ActionResult.CONSUME;
		}
		serverPlayer.openHandledScreen(new ProjectScreenHandlerFactory(project));
		return ActionResult.CONSUME;
	}

	private ProjectData ensureProjectCreated(ServerWorld world, BlockPos pos, ServerPlayerEntity owner) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof ProjectBlockEntity projectEntity)) {
			return null;
		}
		ProjectStore store = ProjectStore.get(world.getServer());
		if (projectEntity.getProjectId() == null) {
			String name = "Project @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			ProjectData project = ProjectData.create(owner.getUuid(), owner.getName().getString(), name);
			store.addProject(project);
			projectEntity.setProjectId(project.getId());
			projectEntity.markDirty();
			return project;
		}
		return store.getProject(projectEntity.getProjectId());
	}
}
