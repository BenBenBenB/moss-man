package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
        MutableText backBtn = TuiHelper.createRunLink(
                TuiHelper.translatable("mossman.command.ticket.back_btn").getString(),
                "/mossman project list",
                TuiHelper.translatable("mossman.command.ticket.back_hover").getString(),
                Formatting.BLUE
        );
        source.sendMessage(createBtn.append(Text.literal(" ")).append(backBtn));

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
        try {
            number = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
             source.sendMessage(Text.literal("Invalid ticket number.").formatted(Formatting.RED));
             return 0;
        }

        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }
        
        var ticketOpt = com.mossman.MossManMod.getTicketRepository().findByProjectId(projectOpt.get().getId()).stream()
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
        source.sendMessage(Text.literal("Priority: " + (ticket.getPriority() != null ? ticket.getPriority().name() : "NONE")).formatted(Formatting.WHITE));
        source.sendMessage(Text.literal("Description: " + (ticket.getDescription() != null ? ticket.getDescription() : "None")).formatted(Formatting.GRAY));

        MutableText updateBtn = TuiHelper.createSuggestLink(
                "[Update Status]",
                "/mossman ticket status " + key + " ",
                "Update ticket status",
                Formatting.GOLD
        );
        source.sendMessage(updateBtn);

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
            int nextNumber = com.mossman.MossManMod.getTicketRepository().getNextTicketNumber(project.getId());
            
            var ticket = new com.mossman.domain.entities.Ticket(
                    0, 
                    project.getId(), 
                    nextNumber, 
                    title, 
                    "", 
                    "Task", 
                    "OPEN", 
                    com.mossman.domain.entities.Priority.MEDIUM, 
                    java.util.Collections.emptyList(), 
                    java.util.Collections.emptyList(), 
                    source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID(), 
                    java.util.Collections.emptyList(), 
                    System.currentTimeMillis(), 
                    System.currentTimeMillis(), 
                    null
            );

            var savedTicket = com.mossman.MossManMod.getCreateTicketUseCase().execute(ticket);
            
            String key = savedTicket.getUserFriendlyKey(prefix);
            source.sendMessage(Text.literal("Created Ticket: " + title + " [" + key + "]").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to create ticket: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int updateStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String newStatus = StringArgumentType.getString(context, "newStatus");
        
        String[] parts = key.split("-");
        if (parts.length != 2) return 0;
        
        String prefix = parts[0];
        int number;
        try { number = Integer.parseInt(parts[1]); } catch (Exception e) { return 0; }

        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll().stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) return 0;
        
        var ticketOpt = com.mossman.MossManMod.getTicketRepository().findByProjectId(projectOpt.get().getId()).stream()
                .filter(t -> t.getTicketNumber() == number)
                .findFirst();

        if (ticketOpt.isEmpty()) {
            source.sendMessage(Text.literal("Ticket not found: " + key).formatted(Formatting.RED));
            return 0;
        }

        try {
            var oldTicket = ticketOpt.get();
            var newTicket = new com.mossman.domain.entities.Ticket(
                    oldTicket.getId(), oldTicket.getProjectId(), oldTicket.getTicketNumber(),
                    oldTicket.getTitle(), oldTicket.getDescription(), oldTicket.getType(),
                    newStatus.toUpperCase(), oldTicket.getPriority(), oldTicket.getAssignees(),
                    oldTicket.getObservers(), oldTicket.getCreator(), oldTicket.getLabels(),
                    oldTicket.getCreatedAt(), System.currentTimeMillis(), oldTicket.getSprintId()
            );
            
            com.mossman.MossManMod.getTicketRepository().save(newTicket);
            source.sendMessage(Text.literal("Updated " + key + " status to " + newStatus.toUpperCase()).formatted(Formatting.GREEN));
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
        source.sendMessage(Text.literal("Added comment to " + key + " (Comments not yet persisted)").formatted(Formatting.YELLOW));
        return 1;
    }
}
