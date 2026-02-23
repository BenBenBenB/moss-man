package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.query.TicketFilter;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var ticketNode = CommandManager.literal("ticket")
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .executes(TicketCommand::listTickets)
                                .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(TicketCommand::listTickets))
                                .then(CommandManager.argument("filter", NbtCompoundArgumentType.nbtCompound())
                                        .executes(TicketCommand::listTicketsFiltered)
                                        .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                                .executes(TicketCommand::listTicketsFiltered)))))
                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(SuggestionHelper::suggestTicketKeys)
                                .executes(TicketCommand::viewTicket)))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("title", StringArgumentType.greedyString())
                                        .executes(TicketCommand::createTicket))))
                .then(CommandManager.literal("update")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(SuggestionHelper::suggestTicketKeys)
                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                        .suggests(SuggestionHelper.suggestTicketPatch("key"))
                                        .executes(TicketCommand::updateTicket))))
                .then(CommandManager.literal("comment")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(SuggestionHelper::suggestTicketKeys)
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(TicketCommand::addComment))))
                .then(CommandManager.literal("assign")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(SuggestionHelper::suggestTicketKeys)
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(TicketCommand::assignTicket))))
                .then(CommandManager.literal("unassign")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(SuggestionHelper::suggestTicketKeys)
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .suggests(SuggestionHelper.suggestTicketAssignees("key"))
                                        .executes(TicketCommand::unassignTicket))))
                .build();

        rootNode.addChild(ticketNode);
    }

    private static int listTickets(CommandContext<ServerCommandSource> context) {
        int page = getPage(context);
        return doListTickets(context, TicketFilter.empty(), page);
    }

    private static int listTicketsFiltered(CommandContext<ServerCommandSource> context) {
        int page = getPage(context);
        NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "filter");
        Map<String, String> filterMap = new HashMap<>();
        // Supported SNBT keys: status, type, priority, title
        // Example: {status:"OPEN",priority:"HIGH"}
        for (String key : nbt.getKeys()) {
            // NbtCompound.getString returns Optional<String> in 1.21.11; use orElse to unwrap
            nbt.getString(key).ifPresent(value -> filterMap.put(key.toLowerCase(), value));
        }
        return doListTickets(context, TicketFilter.of(filterMap), page);
    }

    private static int getPage(CommandContext<ServerCommandSource> context) {
        try {
            return com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException e) {
            return 1;
        }
    }

    private static int doListTickets(CommandContext<ServerCommandSource> context, TicketFilter filter, int page) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
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
                ? TuiHelper.translatable("mossman.tui.ticket.list.header", prefix).getString() + " [filtered]"
                : TuiHelper.translatable("mossman.tui.ticket.list.header", prefix).getString();
        source.sendMessage(Text.literal(headerText).formatted(Formatting.AQUA));
        
        int pageSize = 10;
        int offset = (page - 1) * pageSize;
        
        var tickets = filter.hasAny()
                ? com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), filter, offset, pageSize)
                : com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), offset, pageSize);
        
        long totalTickets = filter.hasAny()
                ? com.mossman.MossManMod.getTicketRepository().countByProjectId(project.getId(), filter)
                : com.mossman.MossManMod.getTicketRepository().countByProjectId(project.getId());
        int totalPages = (int) Math.ceil((double) totalTickets / pageSize);

        if (tickets.isEmpty()) {
            source.sendMessage(Text.literal("No tickets found.").formatted(Formatting.GRAY));
        } else {
            for (var ticket : tickets) {
                String key = ticket.getUserFriendlyKey(prefix);
                MutableText ticketLink = TuiHelper.createRunLink(
                        "[" + key + "]", 
                        "/mossman ticket view " + key, 
                        TuiHelper.translatable("mossman.tui.ticket.view_hover").getString(), 
                        Formatting.GREEN
                ).append(Text.literal(" " + ticket.getTitle() + " [" + ticket.getStatus() + "]").formatted(Formatting.WHITE));
                source.sendMessage(ticketLink);
            }
        }
        
        // Pagination footer
        if (totalPages > 1) {
            MutableText nav = Text.empty();
            if (page > 1) {
                String prevCmd = filter.hasAny() 
                    ? String.format("/mossman ticket list %s %s %d", prefix, getFilterNbt(context), page - 1)
                    : String.format("/mossman ticket list %s %d", prefix, page - 1);
                nav.append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.prev").getString(), prevCmd, "Previous Page", Formatting.GOLD)).append(" ");
            }
            nav.append(TuiHelper.translatable("mossman.tui.common.pagination.page_info", page, totalPages).formatted(Formatting.GRAY));
            if (page < totalPages) {
                String nextCmd = filter.hasAny()
                    ? String.format("/mossman ticket list %s %s %d", prefix, getFilterNbt(context), page + 1)
                    : String.format("/mossman ticket list %s %d", prefix, page + 1);
                nav.append(" ").append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.next").getString(), nextCmd, "Next Page", Formatting.GOLD));
            }
            source.sendMessage(nav);
        }
        
        source.sendMessage(TuiHelper.translatable("mossman.tui.ticket.list.footer").formatted(Formatting.GRAY));
        
        MutableText createBtn = TuiHelper.createSuggestLink(
                TuiHelper.translatable("mossman.tui.ticket.create_btn").getString(),
                "/mossman ticket create " + prefix + " ",
                TuiHelper.translatable("mossman.tui.ticket.create_hover").getString(),
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

        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
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

        var ticketOpt = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                .filter(t -> t.getTicketNumber() == number)
                .findFirst();

        if (ticketOpt.isEmpty()) {
            source.sendMessage(Text.literal("Ticket not found: " + key).formatted(Formatting.RED));
            return 0;
        }
        
        var ticket = ticketOpt.get();
        source.sendMessage(Text.literal("--- Ticket: " + key + " ---").formatted(Formatting.AQUA));
        
        boolean isEditor = com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR);
        
        // Title
        MutableText titleLine = Text.empty();
        if (isEditor) {
            titleLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " {title:\"" + ticket.getTitle() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Title"), Formatting.GRAY));
        }
        titleLine.append(Text.literal("Title: " + ticket.getTitle()).formatted(Formatting.WHITE));
        source.sendMessage(titleLine);

        // Status
        MutableText statusLine = Text.empty();
        if (isEditor) {
            statusLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " {status:\"" + ticket.getStatus() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Status"), Formatting.GRAY));
        }
        statusLine.append(Text.literal("Status: " + ticket.getStatus()).formatted(Formatting.YELLOW));
        source.sendMessage(statusLine);

        // Description
        MutableText descLine = Text.empty();
        if (isEditor) {
            descLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " {description:\"" + (ticket.getDescription() != null ? ticket.getDescription() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Description"), Formatting.GRAY));
        }
        descLine.append(Text.literal("Description: " + (ticket.getDescription() != null ? ticket.getDescription() : "None")).formatted(Formatting.GRAY));
        source.sendMessage(descLine);

        // Assignees
        MutableText assigneesLine = Text.literal("Assignees: ").formatted(Formatting.GRAY);
        if (ticket.getAssignees().isEmpty()) {
            assigneesLine.append(Text.literal("None").formatted(Formatting.GRAY));
        } else {
            for (UUID assigneeId : ticket.getAssignees()) {
                String username = resolveUsername(assigneeId, project, source);
                assigneesLine.append(Text.literal(username).formatted(Formatting.WHITE));
                if (isEditor) {
                    assigneesLine.append(Text.literal(" ").formatted(Formatting.WHITE));
                    assigneesLine.append(TuiHelper.createRunLink("[✗]", "/mossman ticket unassign " + key + " " + username, "Unassign " + username, Formatting.RED));
                }
                assigneesLine.append(Text.literal(" ").formatted(Formatting.WHITE));
            }
        }
        source.sendMessage(assigneesLine);
        if (isEditor) {
            source.sendMessage(TuiHelper.createSuggestLink("[+ Assign] ", "/mossman ticket assign " + key + " ", "Assign a player", Formatting.GOLD));
        }

        if (com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR)) {
            MutableText editBtn = TuiHelper.createSuggestLink("[Edit] ", "/mossman ticket update " + key + " ", "Edit ticket fields", Formatting.YELLOW);
            source.sendMessage(editBtn);
        }

        return 1;
    }

    private static int createTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String title = StringArgumentType.getString(context, "title");
        
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
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
            
            String key = savedTicket.getUserFriendlyKey(prefix);
            MutableText response = TuiHelper.translatable("mossman.tui.ticket.created", title).formatted(Formatting.GREEN);
            response.append(TuiHelper.createRunLink(
                    "[" + key + "]",
                    "/mossman ticket view " + key,
                    TuiHelper.translatable("mossman.tui.ticket.view_hover").getString(),
                    Formatting.GOLD
            ));
            source.sendMessage(response);
            
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }



    private static int addComment(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String message = StringArgumentType.getString(context, "message");

        try {
            String prefix = key.split("-")[0];
            var project = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);

            if (project != null) {
                com.mossman.domain.auth.PermissionChecker.require(project, source, com.mossman.domain.entities.Permission.CREATOR);
            }
            source.sendMessage(Text.literal("Added comment to " + key + " (Comments not yet persisted)").formatted(Formatting.YELLOW));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int assignTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        try {
            String prefix = key.split("-")[0];
            int number = Integer.parseInt(key.split("-")[1]);
            var project = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            var ticket = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                    .filter(t -> t.getTicketNumber() == number).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var profile : GameProfileArgumentType.getProfileArgument(context, "player")) {
                com.mossman.MossManMod.getAssignTicketUseCase().execute(ticket.getId(), requesterId, profile.id());
                source.sendMessage(Text.literal("Assigned " + profile.name() + " to " + key).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int unassignTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        try {
            String prefix = key.split("-")[0];
            int number = Integer.parseInt(key.split("-")[1]);
            var project = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            var ticket = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                    .filter(t -> t.getTicketNumber() == number).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var profile : GameProfileArgumentType.getProfileArgument(context, "player")) {
                com.mossman.MossManMod.getUnassignTicketUseCase().execute(ticket.getId(), requesterId, profile.id());
                source.sendMessage(Text.literal("Unassigned " + profile.name() + " from " + key).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
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
            var project = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            var ticket = com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                    .filter(t -> t.getTicketNumber() == number).findFirst().orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

            java.util.UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID();
            var patch = NbtPatchParser.toMap(nbt);
            com.mossman.MossManMod.getUpdateTicketUseCase().execute(ticket.getId(), rId, patch);
            
            source.sendMessage(Text.literal("Updated ticket " + key).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
    /**
     * Resolves a UUID to a display name.
     * Priority: project member list → online player → short UUID fallback.
     */
    private static String resolveUsername(UUID uuid, com.mossman.domain.entities.Project project, ServerCommandSource source) {
        var member = project.getMembers().stream()
                .filter(m -> m.uuid().equals(uuid))
                .findFirst();
        if (member.isPresent()) return member.get().username();
        var online = source.getServer().getPlayerManager().getPlayer(uuid);
        if (online != null) return online.getName().getString();
        return uuid.toString().substring(0, 8) + "...";
    }

    private static String getFilterNbt(CommandContext<ServerCommandSource> context) {
        try {
            return NbtCompoundArgumentType.getNbtCompound(context, "filter").toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
