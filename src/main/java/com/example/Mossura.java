package com.example;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.network.MossuraNetwork;
import com.example.network.MossuraPayloads;
import com.example.registry.MossuraBlocks;
import com.example.registry.MossuraItems;
import com.example.registry.MossuraScreens;
import com.example.registry.MossuraVillagers;

public class Mossura implements ModInitializer {
	public static final String MOD_ID = "mossura";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		MossuraPayloads.register();
		MossuraBlocks.register();
		MossuraItems.register();
		MossuraScreens.register();
		MossuraVillagers.register();
		MossuraNetwork.registerServerReceivers();
		LOGGER.info("Mossura initialized");
	}
}
