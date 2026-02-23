package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.MossManMod;
import com.mossman.adapters.tui.TuiHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AdminCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var adminNode = CommandManager.literal("admin")
                .then(CommandManager.literal("wipe")
                        .executes(AdminCommand::wipeDatabase))
                .build();

        rootNode.addChild(adminNode);
    }

    private static int wipeDatabase(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendMessage(Text.literal("Wiping MossMan database...").formatted(Formatting.YELLOW));
            MossManMod.getDatabaseManager().dropAndRecreateAllTables();
            source.sendMessage(Text.literal("Database wiped and reinitialized successfully.").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
}
