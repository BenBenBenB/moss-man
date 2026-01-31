package com.example.registry;

import com.example.Mossura;
import com.example.block.ProjectBlock;
import com.example.block.entity.ProjectBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class MossuraBlocks {
	public static final RegistryKey<Block> PROJECT_BLOCK_KEY = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Mossura.MOD_ID, "project_block"));
	public static final Block PROJECT_BLOCK = new ProjectBlock(Block.Settings.copy(Blocks.IRON_BLOCK).registryKey(PROJECT_BLOCK_KEY));
	public static BlockEntityType<ProjectBlockEntity> PROJECT_BLOCK_ENTITY;

	public static void register() {
		Registry.register(Registries.BLOCK, Identifier.of(Mossura.MOD_ID, "project_block"), PROJECT_BLOCK);
		PROJECT_BLOCK_ENTITY = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				Identifier.of(Mossura.MOD_ID, "project_block"),
				FabricBlockEntityTypeBuilder.create(ProjectBlockEntity::new, PROJECT_BLOCK).build()
		);
	}
}
