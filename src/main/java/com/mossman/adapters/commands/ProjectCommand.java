package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.MossManMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;

import java.util.UUID;
import java.util.Collection;

public class ProjectCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var projectNode = CommandManager.literal("project")
                .then(CommandManager.literal("list")
                        .executes(ProjectCommand::listProjects)
                        .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .executes(ProjectCommand::listProjects)))
                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .executes(ProjectCommand::viewProject)))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ProjectCommand::createProject))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .executes(ProjectCommand::deleteProject)))
                .then(CommandManager.literal("update")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                        .suggests(SuggestionHelper.suggestProjectPatch())
                                        .executes(ProjectCommand::updateProject))))
                .then(CommandManager.literal("member")
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectCommand::listMembers)
                                        .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                                .executes(ProjectCommand::listMembers))))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("permission", StringArgumentType.word()).suggests(SuggestionHelper::suggestAssignablePermissions)
                                                        .executes(ProjectCommand::addMember)))))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectCommand::viewMember))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(SuggestionHelper.suggestMemberPatch())
                                                        .executes(ProjectCommand::updateMember)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectCommand::removeMember))))
                        .then(CommandManager.literal("transfer")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectCommand::transferOwnership)))))
                .then(CommandManager.literal("status")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ProjectCommand::addStatus))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectCommand::listStatuses)))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestStatusNames("prefix"))
                                                .executes(ProjectCommand::viewStatus))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestStatusNames("prefix"))
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(SuggestionHelper.suggestStatusPatch())
                                                        .executes(ProjectCommand::updateStatus)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestStatusNames("prefix"))
                                                .executes(ProjectCommand::removeStatus)))))
                .then(CommandManager.literal("ticketType")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ProjectCommand::addTicketType))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectCommand::listTicketTypes)))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestTicketTypeNames("prefix"))
                                                .executes(ProjectCommand::viewTicketType))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestTicketTypeNames("prefix"))
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(SuggestionHelper.suggestTicketTypePatch())
                                                        .executes(ProjectCommand::updateTicketType)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestTicketTypeNames("prefix"))
                                                .executes(ProjectCommand::removeTicketType)))))
                .then(CommandManager.literal("relationshipType")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ProjectCommand::addRelationshipType))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectCommand::listRelationshipTypes)))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestRelationshipTypeNames("prefix"))
                                                .executes(ProjectCommand::viewRelationshipType))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestRelationshipTypeNames("prefix"))
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(SuggestionHelper.suggestRelationshipTypePatch())
                                                        .executes(ProjectCommand::updateRelationshipType)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(SuggestionHelper.suggestRelationshipTypeNames("prefix"))
                                                .executes(ProjectCommand::removeRelationshipType)))))
                .build();

        rootNode.addChild(projectNode);
    }

    private static int listProjects(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendMessage(TuiHelper.translatable("mossman.tui.project.list.header").formatted(Formatting.AQUA));
        
        int page = 1;
        try { page = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "page"); } catch (Exception ignored) {}
        
        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        // Note: For projects, we filter in memory because of the complex permission check
        // In a real app, we'd want to do this in the DB
        var allProjects = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE);
        var visibleProjects = allProjects.stream()
                .filter(p -> PermissionChecker.canView(p, source))
                .toList();

        int totalVisible = visibleProjects.size();
        int totalPages = (int) Math.ceil((double) totalVisible / pageSize);
        
        var paginatedProjects = visibleProjects.stream()
                .skip(offset)
                .limit(pageSize)
                .toList();

        if (paginatedProjects.isEmpty()) {
            source.sendMessage(Text.literal("No projects available to view.").formatted(Formatting.GRAY));
        } else {
            for (var project : paginatedProjects) {
                MutableText projectLink = TuiHelper.createRunLink(
                        "[" + project.getTicketPrefix() + "]", 
                        "/mossman project view " + project.getTicketPrefix(), 
                        TuiHelper.translatable("mossman.tui.project.view_hover").getString(), 
                        Formatting.GREEN
                ).append(Text.literal(" " + project.getName()).formatted(Formatting.WHITE));
                source.sendMessage(projectLink);
            }
        }
        
        // Pagination footer
        if (totalPages > 1) {
            MutableText nav = Text.empty();
            if (page > 1) {
                nav.append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.prev").getString(), "/mossman project list " + (page - 1), "Previous Page", Formatting.GOLD)).append(" ");
            }
            nav.append(TuiHelper.translatable("mossman.tui.common.pagination.page_info", page, totalPages).formatted(Formatting.GRAY));
            if (page < totalPages) {
                nav.append(" ").append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.next").getString(), "/mossman project list " + (page + 1), "Next Page", Formatting.GOLD));
            }
            source.sendMessage(nav);
        }
        
        source.sendMessage(TuiHelper.translatable("mossman.tui.project.list.footer").formatted(Formatting.GRAY));
        
        MutableText createBtn = TuiHelper.createSuggestLink(
                TuiHelper.translatable("mossman.tui.project.create_btn").getString(),
                "/mossman project create ",
                TuiHelper.translatable("mossman.tui.project.create_hover").getString(),
                Formatting.GOLD
        );
        source.sendMessage(createBtn);

        return 1;
    }

    private static int viewProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }
        
        var project = projectOpt.get();
        if (!PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view this project.").formatted(Formatting.RED));
            return 0;
        }

        source.sendMessage(Text.literal("--- Project: [" + project.getTicketPrefix() + "] ---").formatted(Formatting.AQUA));
        
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        // Ticket Prefix
        MutableText prefixLine = Text.empty();
        if (isEditor) {
            prefixLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {ticketPrefix:\"" + project.getTicketPrefix() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Ticket Prefix"), Formatting.GRAY));
        }
        prefixLine.append(Text.literal("Ticket Prefix: " + project.getTicketPrefix()).formatted(Formatting.WHITE));
        source.sendMessage(prefixLine);

        // Name
        MutableText nameLine = Text.empty();
        if (isEditor) {
            nameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {name:\"" + project.getName() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Name"), Formatting.GRAY));
        }
        nameLine.append(Text.literal("Name: " + project.getName()).formatted(Formatting.WHITE));
        source.sendMessage(nameLine);

        // Description
        MutableText descLine = Text.empty();
        if (isEditor) {
            descLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {description:\"" + (project.getDescription() != null ? project.getDescription() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Description"), Formatting.GRAY));
        }
        descLine.append(Text.literal("Description: ").formatted(Formatting.GRAY).append(Text.literal(project.getDescription() != null ? project.getDescription() : "None").formatted(Formatting.WHITE)));
        source.sendMessage(descLine);

        // Icon Texture
        MutableText iconLine = Text.empty();
        if (isEditor) {
            iconLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {iconTexture:\"" + (project.getIconTexture() != null ? project.getIconTexture() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Icon Texture"), Formatting.GRAY));
        }
        iconLine.append(Text.literal("Icon Texture: " + (project.getIconTexture() != null ? project.getIconTexture() : "None")).formatted(Formatting.WHITE));
        source.sendMessage(iconLine);

        // Text Color
        MutableText colorLine = Text.empty();
        if (isEditor) {
            colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {textColor:\"" + (project.getTextColor() != null ? project.getTextColor() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Text Color"), Formatting.GRAY));
        }
        colorLine.append(Text.literal("Text Color: " + (project.getTextColor() != null ? project.getTextColor() : "None")).formatted(Formatting.WHITE));
        source.sendMessage(colorLine);

        // External User Permission
        MutableText extPermLine = Text.empty();
        if (isEditor) {
            extPermLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {externalUserPermission:\"" + project.getExternalUserPermission().name() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "External User Permission"), Formatting.GRAY));
        }
        extPermLine.append(Text.literal("External Permission: " + project.getExternalUserPermission().name()).formatted(Formatting.WHITE));
        source.sendMessage(extPermLine);
        
        MutableText membersLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project member list " + project.getTicketPrefix(), "View project members", Formatting.GOLD);
        source.sendMessage(Text.literal("Members: ").formatted(Formatting.GRAY).append(Text.literal(project.getMembers().size() + " ").formatted(Formatting.WHITE)).append(membersLink));

        MutableText statusesLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project status list " + project.getTicketPrefix(), "View project statuses", Formatting.GOLD);
        source.sendMessage(Text.literal("Statuses: ").formatted(Formatting.GRAY).append(Text.literal(project.getStatuses().size() + " ").formatted(Formatting.WHITE)).append(statusesLink));

        MutableText ticketTypesLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project ticketType list " + project.getTicketPrefix(), "View project ticket types", Formatting.GOLD);
        source.sendMessage(Text.literal("Ticket Types: ").formatted(Formatting.GRAY).append(Text.literal(project.getTicketTypes().size() + " ").formatted(Formatting.WHITE)).append(ticketTypesLink));

        MutableText relTypesLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project relationshipType list " + project.getTicketPrefix(), "View project relationship types", Formatting.GOLD);
        source.sendMessage(Text.literal("Relationship Types: ").formatted(Formatting.GRAY).append(Text.literal(project.getRelationshipTypes().size() + " ").formatted(Formatting.WHITE)).append(relTypesLink));

        
        MutableText viewTicketsBtn = TuiHelper.createRunLink("[View Tickets] ", "/mossman ticket list " + project.getTicketPrefix(), "View tickets", Formatting.YELLOW);
        
        source.sendMessage(viewTicketsBtn);
        source.sendMessage(TuiHelper.translatable("mossman.tui.project.list.footer").formatted(Formatting.GRAY));
        
        return 1;
    }

    private static int createProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        
        if (MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().anyMatch(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))) {
            source.sendMessage(Text.literal("A project with prefix " + prefix + " already exists.").formatted(Formatting.RED));
            return 0;
        }

        try {
            var projectBuilder = com.mossman.domain.entities.Project.builder()
                    .ticketPrefix(prefix.toUpperCase())
                    .name(name);
            
            UUID creatorId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.nameUUIDFromBytes("mossman-system".getBytes());
            String creatorName = source.getPlayer() != null ? source.getPlayer().getName().getString() : "Server";

            String upperPrefix = prefix.toUpperCase();
            MossManMod.getCreateProjectUseCase().execute(projectBuilder, creatorId, creatorName);
            
            MutableText response = TuiHelper.translatable("mossman.tui.project.created", name).formatted(Formatting.GREEN);
            response.append(TuiHelper.createRunLink(
                    "[" + upperPrefix + "]",
                    "/mossman project view " + upperPrefix,
                    TuiHelper.translatable("mossman.tui.project.view_hover").getString(),
                    Formatting.GOLD
            ));
            source.sendMessage(response);
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to create project: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int deleteProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst().orElse(null);

        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            if (!PermissionChecker.isOwner(project, source)) {
                source.sendMessage(Text.literal("Only the project owner can delete the project.").formatted(Formatting.RED));
                return 0;
            }
            MossManMod.getProjectRepository().delete(project.getId());
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
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst().orElse(null);

        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
            MossManMod.getUpdateProjectUseCase().execute(project.getId(), rId, patch);
            source.sendMessage(Text.literal("Updated project [" + project.getTicketPrefix() + "]").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Failed to update project: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int listMembers(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst().orElse(null);

        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        if (!PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view members.").formatted(Formatting.RED));
            return 0;
        }

        int page = 1;
        try { page = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "page"); } catch (Exception ignored) {}
        
        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        var members = MossManMod.getMemberRepository().findByProjectId(project.getId(), offset, pageSize);
        long totalMembers = MossManMod.getMemberRepository().countByProjectId(project.getId());
        int totalPages = (int) Math.ceil((double) totalMembers / pageSize);

        source.sendMessage(Text.literal("--- Members of " + project.getName() + " ---").formatted(Formatting.AQUA));
        if (members.isEmpty()) {
            source.sendMessage(Text.literal("No members found.").formatted(Formatting.GRAY));
        } else {
            for (var member : members) {
                MutableText mText = TuiHelper.createRunLink("[" + member.username() + "] ", "/mossman project member view " + prefix + " " + member.username(), "View member details", Formatting.GREEN)
                        .append(Text.literal(member.username() + " (").formatted(Formatting.WHITE))
                        .append(Text.literal(member.permission().name()).formatted(Formatting.YELLOW))
                        .append(Text.literal(") ").formatted(Formatting.WHITE));
                if (member.title() != null && !member.title().isEmpty()) {
                    mText.append(Text.literal(member.title()).formatted(Formatting.GRAY));
                }
                source.sendMessage(mText);
            }
        }

        if (PermissionChecker.isOwner(project, source)) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Member] ", "/mossman project member add " + prefix + " ", "Click to add a new member", Formatting.GOLD));
        }

        // Pagination footer
        if (totalPages > 1) {
            MutableText nav = Text.empty();
            if (page > 1) {
                nav.append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.prev").getString(), String.format("/mossman project member list %s %d", prefix, page - 1), "Previous Page", Formatting.GOLD)).append(" ");
            }
            nav.append(TuiHelper.translatable("mossman.tui.common.pagination.page_info", page, totalPages).formatted(Formatting.GRAY));
            if (page < totalPages) {
                nav.append(" ").append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.next").getString(), String.format("/mossman project member list %s %d", prefix, page + 1), "Next Page", Formatting.GOLD));
            }
            source.sendMessage(nav);
        }

        return 1;
    }

    private static int viewMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        var project = projectOpt.get();
        if (!PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view members.").formatted(Formatting.RED));
            return 0;
        }

        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        try {
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");

            for (var profile : targetProfiles) {
                var memberOpt = MossManMod.getMemberRepository().findByProjectIdAndUuid(project.getId(), profile.id());
                if (memberOpt.isEmpty()) {
                    source.sendMessage(Text.literal("Member not found in project: " + profile.name()).formatted(Formatting.RED));
                    continue;
                }
                var member = memberOpt.get();

                source.sendMessage(Text.literal("--- Member: " + member.username() + " ---").formatted(Formatting.AQUA));

                // Title
                MutableText titleLine = Text.empty();
                if (isEditor) {
                    titleLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project member update " + prefix + " " + member.username() + " {title:\"" + (member.title() != null ? member.title() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Title"), Formatting.GRAY));
                }
                titleLine.append(Text.literal("Title: " + (member.title() != null ? member.title() : "None")).formatted(Formatting.WHITE));
                source.sendMessage(titleLine);

                // Permission
                MutableText permLine = Text.empty();
                if (isEditor && PermissionChecker.isOwner(project, source)) {
                    permLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project member update " + prefix + " " + member.username() + " {permission:\"" + member.permission().name() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Permission"), Formatting.GRAY));
                }
                permLine.append(Text.literal("Permission: " + member.permission().name()).formatted(Formatting.YELLOW));
                source.sendMessage(permLine);
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Error viewing member: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int addMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
            Permission perm = Permission.valueOf(StringArgumentType.getString(context, "permission").toUpperCase());
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var entry : targetProfiles) {
                var member = new com.mossman.domain.entities.Member(0, entry.id(), entry.name(), "", perm);
                MossManMod.getAddMemberUseCase().execute(project.getId(), rId, member);
                source.sendMessage(Text.literal("Added " + entry.name() + " as " + perm).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Error adding member: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int updateMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
            var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var entry : targetProfiles) {
                MossManMod.getUpdateMemberUseCase().execute(project.getId(), rId, entry.id(), patch);
                source.sendMessage(Text.literal("Updated member " + entry.name()).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Error updating member: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int listStatuses(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null) { source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED)); return 0; }
        if (!PermissionChecker.canView(project, source)) { source.sendMessage(Text.literal("No permission.").formatted(Formatting.RED)); return 0; }

        source.sendMessage(Text.literal("--- Statuses of " + project.getName() + " ---").formatted(Formatting.AQUA));
        boolean isEditorStatus = PermissionChecker.hasPermission(project, source, Permission.EDITOR);
        if (project.getStatuses().isEmpty()) {
            source.sendMessage(Text.literal("No statuses found.").formatted(Formatting.GRAY));
        } else {
            for (var status : project.getStatuses()) {
                MutableText line = Text.empty();
                if (isEditorStatus) {
                    line.append(TuiHelper.createRunLink("[View] ", "/mossman project status view " + prefix + " " + status.name(), "View " + status.name(), Formatting.GOLD));
                    line.append(TuiHelper.createRunLink("[✗] ", "/mossman project status remove " + prefix + " " + status.name(), "Remove " + status.name(), Formatting.RED));
                }
                line.append(coloredName(status.name(), status.textColor()));
                source.sendMessage(line);
            }
        }

        if (isEditorStatus) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Status] ", "/mossman project status add " + prefix + " ", "Click to add a new status", Formatting.GOLD));
        }
        return 1;
    }

    private static int viewStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;
        var status = project.getStatuses().stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (status == null) { source.sendMessage(Text.literal("Status not found: " + name).formatted(Formatting.RED)); return 0; }

        source.sendMessage(Text.literal("--- Status: " + status.name() + " ---").formatted(Formatting.AQUA));
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        MutableText nameLine = Text.empty();
        if (isEditor) nameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project status update " + prefix + " " + status.name() + " {name:\"" + status.name() + "\"}", "Click to edit Name", Formatting.GRAY));
        source.sendMessage(nameLine.append(Text.literal("Name: " + status.name()).formatted(Formatting.WHITE)));

        MutableText colorLine = Text.empty();
        if (isEditor) colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project status update " + prefix + " " + status.name() + " {textColor:\"" + (status.textColor() != null ? status.textColor() : "") + "\"}", "Click to edit Text Color", Formatting.GRAY));
        source.sendMessage(colorLine.append(Text.literal("Text Color: " + (status.textColor() != null ? status.textColor() : "None")).formatted(Formatting.WHITE)));

        return 1;
    }

    private static int updateStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
        var target = project.getStatuses().stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return 0;

        java.util.List<com.mossman.domain.entities.Status> newStatuses = new java.util.ArrayList<>(project.getStatuses());
        newStatuses.remove(target);
        newStatuses.add(new com.mossman.domain.entities.Status(
            patch.getOrDefault("name", target.name()),
            patch.getOrDefault("textColor", target.textColor())
        ));
        
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectStatusesUseCase().execute(project.getId(), rId, newStatuses);
            source.sendMessage(Text.literal("Updated status").formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int addStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;
        
        if (project.getStatuses().stream().anyMatch(s -> s.name().equalsIgnoreCase(name))) {
            source.sendMessage(Text.literal("Status already exists.").formatted(Formatting.RED));
            return 0;
        }

        java.util.List<com.mossman.domain.entities.Status> newStatuses = new java.util.ArrayList<>(project.getStatuses());
        newStatuses.add(new com.mossman.domain.entities.Status(name, ""));
        
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectStatusesUseCase().execute(project.getId(), rId, newStatuses);
            source.sendMessage(Text.literal("Added status " + name).formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int listTicketTypes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;

        source.sendMessage(Text.literal("--- Ticket Types of " + project.getName() + " ---").formatted(Formatting.AQUA));
        boolean isEditorTT = PermissionChecker.hasPermission(project, source, Permission.EDITOR);
        if (project.getTicketTypes().isEmpty()) {
            source.sendMessage(Text.literal("No ticket types found.").formatted(Formatting.GRAY));
        } else {
            for (var tt : project.getTicketTypes()) {
                MutableText line = Text.empty();
                if (isEditorTT) {
                    line.append(TuiHelper.createRunLink("[View] ", "/mossman project ticketType view " + prefix + " " + tt.name(), "View " + tt.name(), Formatting.GOLD));
                    line.append(TuiHelper.createRunLink("[✗] ", "/mossman project ticketType remove " + prefix + " " + tt.name(), "Remove " + tt.name(), Formatting.RED));
                }
                line.append(coloredName(tt.name(), tt.textColor()));
                source.sendMessage(line);
            }
        }

        if (isEditorTT) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Ticket Type] ", "/mossman project ticketType add " + prefix + " ", "Click to add a new ticket type", Formatting.GOLD));
        }
        return 1;
    }

    private static int viewTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;
        var tt = project.getTicketTypes().stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (tt == null) return 0;

        source.sendMessage(Text.literal("--- Ticket Type: " + tt.name() + " ---").formatted(Formatting.AQUA));
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        MutableText nameLine = Text.empty();
        if (isEditor) nameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project ticketType update " + prefix + " " + tt.name() + " {name:\"" + tt.name() + "\"}", "Click to edit Name", Formatting.GRAY));
        source.sendMessage(nameLine.append(Text.literal("Name: " + tt.name()).formatted(Formatting.WHITE)));

        MutableText colorLine = Text.empty();
        if (isEditor) colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project ticketType update " + prefix + " " + tt.name() + " {textColor:\"" + (tt.textColor() != null ? tt.textColor() : "") + "\"}", "Click to edit Text Color", Formatting.GRAY));
        source.sendMessage(colorLine.append(Text.literal("Text Color: " + (tt.textColor() != null ? tt.textColor() : "None")).formatted(Formatting.WHITE)));
        return 1;
    }

    private static int updateTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
        var target = project.getTicketTypes().stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return 0;
        
        java.util.List<com.mossman.domain.entities.TicketType> newLists = new java.util.ArrayList<>(project.getTicketTypes());
        newLists.remove(target);
        newLists.add(new com.mossman.domain.entities.TicketType(patch.getOrDefault("name", target.name()), patch.getOrDefault("textColor", target.textColor())));
        
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectTicketTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Updated ticket type").formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int addTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;
        
        if (project.getTicketTypes().stream().anyMatch(t -> t.name().equalsIgnoreCase(name))) {
            source.sendMessage(Text.literal("Ticket type already exists.").formatted(Formatting.RED));
            return 0;
        }

        java.util.List<com.mossman.domain.entities.TicketType> newLists = new java.util.ArrayList<>(project.getTicketTypes());
        newLists.add(new com.mossman.domain.entities.TicketType(name, ""));
        
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectTicketTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Added ticket type " + name).formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int listRelationshipTypes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;

        source.sendMessage(Text.literal("--- Relationship Types of " + project.getName() + " ---").formatted(Formatting.AQUA));
        boolean isEditorRT = PermissionChecker.hasPermission(project, source, Permission.EDITOR);
        if (project.getRelationshipTypes().isEmpty()) {
            source.sendMessage(Text.literal("No relationship types found.").formatted(Formatting.GRAY));
        } else {
            for (var rt : project.getRelationshipTypes()) {
                MutableText line = Text.empty();
                if (isEditorRT) {
                    line.append(TuiHelper.createRunLink("[View] ", "/mossman project relationshipType view " + prefix + " " + rt.name(), "View " + rt.name(), Formatting.GOLD));
                    line.append(TuiHelper.createRunLink("[✗] ", "/mossman project relationshipType remove " + prefix + " " + rt.name(), "Remove " + rt.name(), Formatting.RED));
                }
                line.append(coloredName(rt.name(), rt.textColor()));
                source.sendMessage(line);
            }
        }

        if (isEditorRT) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Relationship Type] ", "/mossman project relationshipType add " + prefix + " ", "Click to add a new relationship type", Formatting.GOLD));
        }
        return 1;
    }

    private static int viewRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;
        var rt = project.getRelationshipTypes().stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (rt == null) return 0;

        source.sendMessage(Text.literal("--- Relationship Type: " + rt.name() + " ---").formatted(Formatting.AQUA));
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        MutableText nameLine = Text.empty();
        if (isEditor) nameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.name() + " {name:\"" + rt.name() + "\"}", "Click to edit Name", Formatting.GRAY));
        source.sendMessage(nameLine.append(Text.literal("Name: " + rt.name()).formatted(Formatting.WHITE)));

        MutableText sourceToTargetLine = Text.empty();
        if (isEditor) sourceToTargetLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.name() + " {sourceToTargetDescription:\"" + (rt.sourceToTargetDescription() != null ? rt.sourceToTargetDescription() : "") + "\"}", "Click to edit Source -> Target", Formatting.GRAY));
        source.sendMessage(sourceToTargetLine.append(Text.literal("Source->Target: " + (rt.sourceToTargetDescription() != null ? rt.sourceToTargetDescription() : "None")).formatted(Formatting.WHITE)));

        MutableText targetToSourceLine = Text.empty();
        if (isEditor) targetToSourceLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.name() + " {targetToSourceDescription:\"" + (rt.targetToSourceDescription() != null ? rt.targetToSourceDescription() : "") + "\"}", "Click to edit Target -> Source", Formatting.GRAY));
        source.sendMessage(targetToSourceLine.append(Text.literal("Target->Source: " + (rt.targetToSourceDescription() != null ? rt.targetToSourceDescription() : "None")).formatted(Formatting.WHITE)));

        MutableText colorLine = Text.empty();
        if (isEditor) colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.name() + " {textColor:\"" + (rt.textColor() != null ? rt.textColor() : "") + "\"}", "Click to edit Text Color", Formatting.GRAY));
        source.sendMessage(colorLine.append(Text.literal("Text Color: " + (rt.textColor() != null ? rt.textColor() : "None")).formatted(Formatting.WHITE)));
        return 1;
    }

    private static int updateRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
        var target = project.getRelationshipTypes().stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return 0;
        
        java.util.List<com.mossman.domain.entities.RelationshipType> newLists = new java.util.ArrayList<>(project.getRelationshipTypes());
        newLists.remove(target);
        newLists.add(new com.mossman.domain.entities.RelationshipType(
            patch.getOrDefault("name", target.name()),
            patch.getOrDefault("sourceToTargetDescription", target.sourceToTargetDescription()),
            patch.getOrDefault("targetToSourceDescription", target.targetToSourceDescription()),
            patch.getOrDefault("textColor", target.textColor())
        ));
        
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectRelationshipTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Updated relationship type").formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int addRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;
        
        if (project.getRelationshipTypes().stream().anyMatch(t -> t.name().equalsIgnoreCase(name))) {
            source.sendMessage(Text.literal("Relationship type already exists.").formatted(Formatting.RED));
            return 0;
        }

        java.util.List<com.mossman.domain.entities.RelationshipType> newLists = new java.util.ArrayList<>(project.getRelationshipTypes());
        newLists.add(new com.mossman.domain.entities.RelationshipType(name, "", "", ""));
        
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectRelationshipTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Added relationship type " + name).formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int removeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var target = project.getStatuses().stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { source.sendMessage(Text.literal("Status not found: " + name).formatted(Formatting.RED)); return 0; }

        java.util.List<com.mossman.domain.entities.Status> newStatuses = new java.util.ArrayList<>(project.getStatuses());
        newStatuses.remove(target);
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectStatusesUseCase().execute(project.getId(), rId, newStatuses);
            source.sendMessage(Text.literal("Removed status " + name).formatted(Formatting.YELLOW));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int removeTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var target = project.getTicketTypes().stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { source.sendMessage(Text.literal("Ticket type not found: " + name).formatted(Formatting.RED)); return 0; }

        java.util.List<com.mossman.domain.entities.TicketType> newLists = new java.util.ArrayList<>(project.getTicketTypes());
        newLists.remove(target);
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectTicketTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Removed ticket type " + name).formatted(Formatting.YELLOW));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int removeRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var target = project.getRelationshipTypes().stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { source.sendMessage(Text.literal("Relationship type not found: " + name).formatted(Formatting.RED)); return 0; }

        java.util.List<com.mossman.domain.entities.RelationshipType> newLists = new java.util.ArrayList<>(project.getRelationshipTypes());
        newLists.remove(target);
        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectRelationshipTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Removed relationship type " + name).formatted(Formatting.YELLOW));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static MutableText coloredName(String name, String textColor) {
        if (textColor != null && !textColor.isBlank()) {
            Formatting fmt = Formatting.byName(textColor.toLowerCase());
            if (fmt != null && fmt.isColor()) return Text.literal(name).formatted(fmt);
            if (textColor.startsWith("#") && textColor.length() == 7) {
                try {
                    int rgb = Integer.parseInt(textColor.substring(1), 16);
                    return Text.literal(name).setStyle(Style.EMPTY.withColor(rgb));
                } catch (NumberFormatException ignored) {}
            }
        }
        return Text.literal(name).formatted(Formatting.WHITE);
    }

    private static com.mossman.domain.entities.Project.Builder cloneProjectWithLists(com.mossman.domain.entities.Project project) {
        return com.mossman.domain.entities.Project.builder()
                .id(project.getId())
                .ticketPrefix(project.getTicketPrefix())
                .name(project.getName())
                .description(project.getDescription())
                .iconTexture(project.getIconTexture())
                .externalUserPermission(project.getExternalUserPermission())
                .textColor(project.getTextColor())
                .statuses(project.getStatuses())
                .ticketTypes(project.getTicketTypes())
                .relationshipTypes(project.getRelationshipTypes())
                .members(project.getMembers());
    }

    private static int removeMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var entry : targetProfiles) {
                MossManMod.getRemoveMemberUseCase().execute(project.getId(), rId, entry.id());
                source.sendMessage(Text.literal("Removed member " + entry.name()).formatted(Formatting.YELLOW));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Error removing member: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int transferOwnership(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var entry = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getTransferOwnershipUseCase().execute(project.getId(), rId, entry.id());
            source.sendMessage(Text.literal("Transferred ownership of " + project.getTicketPrefix() + " to " + entry.name()).formatted(Formatting.GOLD));
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.literal("Error transferring ownership: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }
}
