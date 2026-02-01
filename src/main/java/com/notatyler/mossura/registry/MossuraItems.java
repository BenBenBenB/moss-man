package com.notatyler.mossura.registry;

import com.notatyler.mossura.Mossura;
import com.notatyler.mossura.item.ProjectAccessDevice;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class MossuraItems {
	public static final RegistryKey<Item> PROJECT_BLOCK_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Mossura.MOD_ID, "project_block"));
	public static final RegistryKey<Item> PAD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Mossura.MOD_ID, "wireless_project_access"));
	public static final Item PROJECT_BLOCK_ITEM = new BlockItem(MossuraBlocks.PROJECT_BLOCK, new Item.Settings().registryKey(PROJECT_BLOCK_ITEM_KEY));
	public static final Item PAD = new ProjectAccessDevice(new Item.Settings().registryKey(PAD_KEY).maxCount(1));

	public static void register() {
		Registry.register(Registries.ITEM, Identifier.of(Mossura.MOD_ID, "project_block"), PROJECT_BLOCK_ITEM);
		Registry.register(Registries.ITEM, Identifier.of(Mossura.MOD_ID, "wireless_project_access"), PAD);
	}
}
