package com.notatyler.mossura.registry;

import com.notatyler.mossura.Mossura;
import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class MossuraVillagers {
	public static PointOfInterestType PROJECT_POI;
	public static final RegistryKey<VillagerProfession> SCRUM_MASTER_KEY = RegistryKey.of(RegistryKeys.VILLAGER_PROFESSION, Identifier.of(Mossura.MOD_ID, "scrum_master"));
	public static VillagerProfession SCRUM_MASTER;

	public static void register() {
		Identifier poiId = Identifier.of(Mossura.MOD_ID, "project_block_poi");
		PROJECT_POI = PointOfInterestHelper.register(poiId, 1, 1, MossuraBlocks.PROJECT_BLOCK);

		SCRUM_MASTER = Registry.register(
				Registries.VILLAGER_PROFESSION,
				Identifier.of(Mossura.MOD_ID, "scrum_master"),
				new VillagerProfession(
						Text.translatable("entity.minecraft.villager.mossura.scrum_master"),
						entry -> entry.value() == PROJECT_POI,
						entry -> entry.value() == PROJECT_POI,
						ImmutableSet.of(),
						ImmutableSet.of(),
						SoundEvents.ENTITY_VILLAGER_WORK_LIBRARIAN
				)
		);

		TradeOfferHelper.registerVillagerOffers(SCRUM_MASTER_KEY, 1, factories -> {
			factories.add((world, entity, random) -> new TradeOffer(
					new TradedItem(net.minecraft.item.Items.EMERALD, 7 + random.nextInt(10)),
					new ItemStack(MossuraBlocks.PROJECT_BLOCK, 1),
					6,
					5,
					0.05f
			));
		});

		TradeOfferHelper.registerVillagerOffers(SCRUM_MASTER_KEY, 2, factories -> {
			factories.add((world, entity, random) -> new TradeOffer(
					new TradedItem(net.minecraft.item.Items.EMERALD, 7 + random.nextInt(10)),
					new ItemStack(MossuraItems.PAD, 1),
					3,
					10,
					0.05f
			));
		});
	}
}
