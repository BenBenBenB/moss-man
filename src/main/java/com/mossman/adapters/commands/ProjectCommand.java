package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.MossManMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
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
                .then(CommandManager.literal("member")
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word())
                                        .executes(ProjectCommand::listMembers)
                                        .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                                .executes(ProjectCommand::listMembers))))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word())
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("permission", StringArgumentType.word())
                                                        .executes(ProjectCommand::addMember)))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word())
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .executes(ProjectCommand::updateMember)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word())
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectCommand::removeMember))))
                        .then(CommandManager.literal("transfer")
                                .then(CommandManager.argument("prefix", StringArgumentType.word())
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectCommand::transferOwnership)))))
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
        
        source.sendMessage(Text.literal("Members: ").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(project.getMembers().size())).formatted(Formatting.WHITE)));
        
        MutableText viewTicketsBtn = TuiHelper.createRunLink("[View Tickets] ", "/mossman ticket list " + project.getTicketPrefix(), "View tickets", Formatting.YELLOW);
        MutableText viewMembersBtn = TuiHelper.createRunLink("[View Members] ", "/mossman project member list " + project.getTicketPrefix(), "View project members", Formatting.GOLD);
        
        source.sendMessage(viewTicketsBtn.append(viewMembersBtn));
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
        for (var member : members) {
            MutableText mText = Text.literal("- " + member.username() + " (").formatted(Formatting.WHITE)
                    .append(Text.literal(member.permission().name()).formatted(Formatting.YELLOW))
                    .append(Text.literal(") ").formatted(Formatting.WHITE));
            if (member.title() != null && !member.title().isEmpty()) {
                mText.append(Text.literal(member.title()).formatted(Formatting.GRAY));
            }
            source.sendMessage(mText);
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
