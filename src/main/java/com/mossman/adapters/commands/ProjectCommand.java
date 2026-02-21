package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.MossManMod;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ProjectCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var projectNode = CommandManager.literal("project")
                .then(CommandManager.literal("list").executes(ProjectCommand::listProjects))
                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .executes(ProjectCommand::viewProject)))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ProjectCommand::createProject))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .executes(ProjectCommand::deleteProject)))
                .then(CommandManager.literal("update")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                        .executes(ProjectCommand::updateProject))))
                .build();

        rootNode.addChild(projectNode);
    }

    private static int listProjects(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendMessage(TuiHelper.translatable("mossman.command.project.list.header").formatted(Formatting.AQUA));
        
        var projects = MossManMod.getProjectRepository().findAll();
        if (projects.isEmpty()) {
            source.sendMessage(Text.literal("No projects found.").formatted(Formatting.GRAY));
        } else {
            for (var project : projects) {
                MutableText projectLink = TuiHelper.createRunLink(
                        "[" + project.getTicketPrefix() + "]", 
                        "/mossman project view " + project.getTicketPrefix(), 
                        TuiHelper.translatable("mossman.command.project.view_hover").getString(), 
                        Formatting.GREEN
                ).append(Text.literal(" " + project.getName()).formatted(Formatting.WHITE));
                source.sendMessage(projectLink);
            }
        }
        
        source.sendMessage(TuiHelper.translatable("mossman.command.project.list.footer").formatted(Formatting.GRAY));
        
        MutableText createBtn = TuiHelper.createSuggestLink(
                TuiHelper.translatable("mossman.command.project.create_btn").getString(),
                "/mossman project create ",
                TuiHelper.translatable("mossman.command.project.create_hover").getString(),
                Formatting.GOLD
        );
        source.sendMessage(createBtn);

        return 1;
    }

    private static int viewProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }
        
        var project = projectOpt.get();
        source.sendMessage(Text.literal("--- Project: " + project.getName() + " [" + project.getTicketPrefix() + "] ---").formatted(Formatting.AQUA));
        
        MutableText viewTicketsBtn = TuiHelper.createRunLink(
                "[View Tickets]",
                "/mossman ticket list " + project.getTicketPrefix(),
                "View tickets for this project",
                Formatting.YELLOW
        );
        source.sendMessage(viewTicketsBtn);
        source.sendMessage(TuiHelper.translatable("mossman.command.project.list.footer").formatted(Formatting.GRAY));
        
        return 1;
    }

    private static int createProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        
        // Basic validation - check if prefix exists
        boolean exists = MossManMod.getProjectRepository().findAll().stream()
                .anyMatch(p -> p.getTicketPrefix().equalsIgnoreCase(prefix));
        
        if (exists) {
            source.sendMessage(Text.literal("A project with prefix " + prefix + " already exists.").formatted(Formatting.RED));
            return 0;
        }

        try {
            var projectBuilder = com.mossman.domain.entities.Project.builder()
                    .ticketPrefix(prefix.toUpperCase())
                    .name(name)
                    .owner(source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID());
            
            MossManMod.getCreateProjectUseCase().execute(projectBuilder);
            
            source.sendMessage(Text.literal("Created Project: " + name + " [" + prefix.toUpperCase() + "]").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to create project: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int deleteProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            MossManMod.getProjectRepository().delete(projectOpt.get().getId());
            source.sendMessage(Text.literal("Deleted Project: " + prefix).formatted(Formatting.RED));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to delete project: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int updateProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var nbt = NbtCompoundArgumentType.getNbtCompound(context, "patch");

        var projectOpt = MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            var patch = NbtPatchParser.toMap(nbt);
            var updated = MossManMod.getUpdateProjectUseCase().execute(projectOpt.get().getId(), patch);
            source.sendMessage(Text.literal("Updated project [" + updated.getTicketPrefix() + "]: " + updated.getName()).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to update project: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }
}
