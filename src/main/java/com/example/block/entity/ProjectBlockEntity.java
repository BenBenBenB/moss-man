package com.example.block.entity;

import com.example.registry.MossuraBlocks;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public class ProjectBlockEntity extends BlockEntity {
	private UUID projectId;

	public ProjectBlockEntity(BlockPos pos, BlockState state) {
		super(MossuraBlocks.PROJECT_BLOCK_ENTITY, pos, state);
	}

	public UUID getProjectId() {
		return projectId;
	}

	public void setProjectId(UUID projectId) {
		this.projectId = projectId;
		markDirty();
	}

	@Override
	protected void writeData(WriteView writeView) {
		super.writeData(writeView);
		if (projectId != null) {
			writeView.putString("projectId", projectId.toString());
		}
	}

	@Override
	protected void readData(ReadView readView) {
		super.readData(readView);
		String value = readView.getString("projectId", "");
		projectId = value.isEmpty() ? null : UUID.fromString(value);
	}
}
