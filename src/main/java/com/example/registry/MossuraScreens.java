package com.example.registry;

import com.example.Mossura;
import com.example.screen.ProjectScreenHandler;
import com.example.screen.ProjectScreenOpenData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class MossuraScreens {
	public static ScreenHandlerType<ProjectScreenHandler> PROJECT_SCREEN_HANDLER;

	public static void register() {
		PROJECT_SCREEN_HANDLER = Registry.register(
				Registries.SCREEN_HANDLER,
				Identifier.of(Mossura.MOD_ID, "project"),
				new ExtendedScreenHandlerType<>(ProjectScreenHandler::new, ProjectScreenOpenData.CODEC)
		);
	}
}
