package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.query.TicketFilter;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public class TicketCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var ticketNode = CommandManager.literal("ticket")
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .executes(TicketCommand::listTickets)
                                .then(CommandManager.argument("filter", NbtCompoundArgumentType.nbtCompound())
                                        .executes(TicketCommand::listTicketsFiltered))))
                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(TicketCommand::viewTicket)))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .then(CommandManager.argument("title", StringArgumentType.greedyString())
                                        .executes(TicketCommand::createTicket))))
                .then(CommandManager.literal("status")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("newStatus", StringArgumentType.word())
                                        .executes(TicketCommand::updateStatus))))
                .then(CommandManager.literal("update")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                        .executes(TicketCommand::updateTicket))))
                .then(CommandManager.literal("comment")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(TicketCommand::addComment))))
                .build();

        rootNode.addChild(ticketNode);
    }

    private static int listTickets(CommandContext<ServerCommandSource> context) {
        return doListTickets(context, TicketFilter.empty());
    }

    private static int listTicketsFiltered(CommandContext<ServerCommandSource> context) {
        NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "filter");
        Map<String, String> filterMap = new HashMap<>();
        // Supported SNBT keys: status, type, priority, title
        // Example: {status:"OPEN",priority:"HIGH"}
        for (String key : nbt.getKeys()) {
            // NbtCompound.getString returns Optional<String> in 1.21.11; use orElse to unwrap
            nbt.getString(key).ifPresent(value -> filterMap.put(key.toLowerCase(), value));
        }
        return doListTickets(context, TicketFilter.of(filterMap));
    }

    private static int doListTickets(CommandContext<ServerCommandSource> context, TicketFilter filter) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        var project = projectOpt.get();
        if (!com.mossman.domain.auth.PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view tickets for this project.").formatted(Formatting.RED));
            return 0;
        }

        var headerText = filter.hasAny()
                ? TuiHelper.translatable("mossman.command.ticket.list.header", prefix).getString() + " [filtered]"
                : TuiHelper.translatable("mossman.command.ticket.list.header", prefix).getString();
        source.sendMessage(Text.literal(headerText).formatted(Formatting.AQUA));
        
        var tickets = filter.hasAny()
                ? com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), filter)
                : com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId());
        
        if (tickets.isEmpty()) {
            source.sendMessage(Text.literal("No tickets found.").formatted(Formatting.GRAY));
        } else {
            for (var ticket : tickets) {
                String key = ticket.getUserFriendlyKey(prefix);
                MutableText ticketLink = TuiHelper.createRunLink(
                        "[" + key + "]", 
                        "/mossman ticket view " + key, 
                        TuiHelper.translatable("mossman.command.ticket.view_hover").getString(), 
                        Formatting.GREEN
                ).append(Text.literal(" " + ticket.getTitle() + " [" + ticket.getStatus() + "]").formatted(Formatting.WHITE));
                source.sendMessage(ticketLink);
            }
        }
        
        source.sendMessage(TuiHelper.translatable("mossman.command.ticket.list.footer").formatted(Formatting.GRAY));
        
        MutableText createBtn = TuiHelper.createSuggestLink(
                TuiHelper.translatable("mossman.command.ticket.create_btn").getString(),
                "/mossman ticket create " + prefix + " ",
                TuiHelper.translatable("mossman.command.ticket.create_hover").getString(),
                Formatting.GOLD
        );
        source.sendMessage(createBtn);

        return 1;
    }

    private static int viewTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        
        String[] parts = key.split("-");
        if (parts.length != 2) {
            source.sendMessage(Text.literal("Invalid ticket key format. Expected PREFIX-NUMBER").formatted(Formatting.RED));
            return 0;
        }
        
        String prefix = parts[0];
        int number;
        try { number = Integer.parseInt(parts[1]); } catch (Exception e) { return 0; }

        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }
        
        var project = projectOpt.get();
        if (!com.mossman.domain.auth.PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view tickets for this project.").formatted(Formatting.RED));
            return 0;
        }

        var ticketOpt = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId()).stream()
                .filter(t -> t.getTicketNumber() == number)
                .findFirst();

        if (ticketOpt.isEmpty()) {
            source.sendMessage(Text.literal("Ticket not found: " + key).formatted(Formatting.RED));
            return 0;
        }
        
        var ticket = ticketOpt.get();
        source.sendMessage(Text.literal("--- Ticket: " + key + " ---").formatted(Formatting.AQUA));
        source.sendMessage(Text.literal("Title: " + ticket.getTitle()).formatted(Formatting.WHITE));
        source.sendMessage(Text.literal("Status: " + ticket.getStatus()).formatted(Formatting.YELLOW));
        source.sendMessage(Text.literal("Description: " + (ticket.getDescription() != null ? ticket.getDescription() : "None")).formatted(Formatting.GRAY));

        if (com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR)) {
            MutableText updateBtn = TuiHelper.createSuggestLink("[Update Status] ", "/mossman ticket status " + key + " ", "Update ticket status", Formatting.GOLD);
            MutableText editBtn = TuiHelper.createSuggestLink("[Edit] ", "/mossman ticket update " + key + " ", "Edit ticket fields", Formatting.YELLOW);
            source.sendMessage(updateBtn.append(editBtn));
        }

        return 1;
    }

    private static int createTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String title = StringArgumentType.getString(context, "title");
        
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            var project = projectOpt.get();
            java.util.UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID();
            int nextNumber = com.mossman.MossManMod.getTicketRepository().getNextTicketNumber(project.getId());
            
            var ticket = new com.mossman.domain.entities.Ticket(
                    0, project.getId(), nextNumber, title, "", "Task", "OPEN", 
                    com.mossman.domain.entities.Priority.MEDIUM, 
                    java.util.Collections.emptyList(), java.util.Collections.emptyList(), 
                    rId, java.util.Collections.emptyList(), 
                    System.currentTimeMillis(), System.currentTimeMillis(), null
            );

            var savedTicket = com.mossman.MossManMod.getCreateTicketUseCase().execute(ticket, rId);
            source.sendMessage(Text.literal("Created Ticket: " + title + " [" + savedTicket.getUserFriendlyKey(prefix) + "]").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to create ticket: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int updateStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String newStatus = StringArgumentType.getString(context, "newStatus").toUpperCase();
        
        try {
            String prefix = key.split("-")[0];
            int number = Integer.parseInt(key.split("-")[1]);

            var project = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            var ticket = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId()).stream()
                    .filter(t -> t.getTicketNumber() == number).findFirst().orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

            java.util.UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID();
            
            // Re-using UpdateTicketUseCase but with status patch
            java.util.Map<String, String> patch = java.util.Map.of("status", newStatus);
            com.mossman.MossManMod.getUpdateTicketUseCase().execute(ticket.getId(), rId, patch);
            
            source.sendMessage(Text.literal("Updated " + key + " status to " + newStatus).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
             source.sendMessage(Text.literal("Failed to update status: " + e.getMessage()).formatted(Formatting.RED));
             return 0;
        }
    }

    private static int addComment(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String message = StringArgumentType.getString(context, "message");

        try {
            String prefix = key.split("-")[0];
            var project = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);

            if (project != null) {
                com.mossman.domain.auth.PermissionChecker.require(project, source, com.mossman.domain.entities.Permission.CREATOR);
            }
            source.sendMessage(Text.literal("Added comment to " + key + " (Comments not yet persisted)").formatted(Formatting.YELLOW));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Error: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int updateTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        var nbt = NbtCompoundArgumentType.getNbtCompound(context, "patch");

        try {
            String prefix = key.split("-")[0];
            int number = Integer.parseInt(key.split("-")[1]);
            var project = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            var ticket = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId()).stream()
                    .filter(t -> t.getTicketNumber() == number).findFirst().orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

            java.util.UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID();
            var patch = NbtPatchParser.toMap(nbt);
            com.mossman.MossManMod.getUpdateTicketUseCase().execute(ticket.getId(), rId, patch);
            
            source.sendMessage(Text.literal("Updated ticket " + key).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to update ticket: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }
}
