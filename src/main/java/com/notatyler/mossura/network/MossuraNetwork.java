package com.notatyler.mossura.network;

import com.notatyler.mossura.Mossura;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.project.ProjectService;
import com.notatyler.mossura.project.ProjectStore;
import com.notatyler.mossura.project.TicketUpdate;
import com.notatyler.mossura.tickets.Subtask;
import com.notatyler.mossura.tickets.Ticket;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MossuraNetwork {
	public static final Identifier PROJECT_SYNC = Identifier.of(Mossura.MOD_ID, "project_sync");
	public static final Identifier CREATE_TICKET = Identifier.of(Mossura.MOD_ID, "create_ticket");
	public static final Identifier UPDATE_TICKET = Identifier.of(Mossura.MOD_ID, "update_ticket");
	public static final Identifier ADD_TICKET_COMMENT = Identifier.of(Mossura.MOD_ID, "add_ticket_comment");
	public static final Identifier ADD_SUBTASK = Identifier.of(Mossura.MOD_ID, "add_subtask");
	public static final Identifier ADD_SUBTASK_COMMENT = Identifier.of(Mossura.MOD_ID, "add_subtask_comment");
	public static final Identifier UPDATE_PROJECT_SETTINGS = Identifier.of(Mossura.MOD_ID, "update_project_settings");
	public static final Identifier ADD_PROJECT_MEMBER = Identifier.of(Mossura.MOD_ID, "add_project_member");
	public static final Identifier REMOVE_PROJECT_MEMBER = Identifier.of(Mossura.MOD_ID, "remove_project_member");

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.CreateTicketPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				java.util.List<UUID> assigneeIds = resolvePlayerIds(context.server(), payload.assigneeNames());
				Ticket ticket = ProjectService.createTicket(context.server(), project, player.getUuid(), player.getName().getString(), payload.title(), payload.description(), payload.priority(), payload.type(), payload.state(), assigneeIds, payload.assigneeNames(), context.server().getOverworld().getTime());
				if (payload.sprintId() != null) {
					ticket.setSprintId(payload.sprintId());
				}
				if (payload.labels() != null) {
					for (String label : payload.labels()) {
						ticket.addLabel(label);
					}
				}
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.UpdateTicketPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				System.out.println("DEBUG: Server received UpdateTicketPayload: " + payload);
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					System.out.println("DEBUG: Project not found on server");
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				java.util.List<UUID> assigneeIds = resolvePlayerIds(context.server(), payload.assigneeNames());
				TicketUpdate update = new TicketUpdate(payload.title(), payload.description(), payload.priority(), payload.type(), payload.state(), assigneeIds, payload.assigneeNames(), payload.sprintId(), payload.labels());
				ProjectService.updateTicket(context.server(), project, ticket, player.getUuid(), player.getName().getString(), update, context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.DeleteTicketPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				ProjectService.deleteTicket(project, ticket, player.getUuid(), player.getName().getString(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.UpdateSubtaskPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				Subtask subtask = ticket.findSubtask(payload.subtaskId());
				if (subtask == null) {
					player.sendMessage(Text.literal("Subtask not found."), false);
					return;
				}
				ProjectService.updateSubtask(ticket, subtask, player.getUuid(), player.getName().getString(), payload.name(), payload.description(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.SetSubtaskCompletePayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				Subtask subtask = ticket.findSubtask(payload.subtaskId());
				if (subtask == null) {
					player.sendMessage(Text.literal("Subtask not found."), false);
					return;
				}
				ProjectService.setSubtaskCompleted(ticket, subtask, player.getUuid(), player.getName().getString(), payload.completed(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.AddTicketCommentPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				ProjectService.addTicketComment(ticket, player.getUuid(), player.getName().getString(), payload.message(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.AddSubtaskPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				ProjectService.addSubtask(ticket, player.getUuid(), player.getName().getString(), payload.name(), payload.description(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.AddSubtaskCommentPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canEdit(project, player)) {
					player.sendMessage(Text.literal("You do not have permission to edit this project."), false);
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				Subtask subtask = ticket.findSubtask(payload.subtaskId());
				if (subtask == null) {
					player.sendMessage(Text.literal("Subtask not found."), false);
					return;
				}
				ProjectService.addSubtaskComment(ticket, subtask, player.getUuid(), player.getName().getString(), payload.message(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.UpdateProjectSettingsPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canAdmin(project, player)) {
					player.sendMessage(Text.literal("Only the project owner or admins can update settings."), false);
					return;
				}
				ProjectService.updateProjectSettings(context.server(), project, payload.name(), payload.description(), payload.ticketPrefix(), payload.statuses(), payload.ticketTypes(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.AddProjectMemberPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canManageMembers(project, player)) {
					player.sendMessage(Text.literal("Only the project owner can manage members."), false);
					return;
				}
				ServerPlayerEntity target = context.server().getPlayerManager().getPlayer(payload.memberName());
				if (target == null) {
					player.sendMessage(Text.literal("Player not found: " + payload.memberName()), false);
					return;
				}
				ProjectService.addMember(project, target.getUuid(), target.getName().getString(), payload.level());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.RemoveProjectMemberPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				if (!canManageMembers(project, player)) {
					player.sendMessage(Text.literal("Only the project owner can manage members."), false);
					return;
				}
				UUID memberId = project.findMemberIdByName(payload.memberName());
				if (memberId == null) {
					player.sendMessage(Text.literal("Member not found: " + payload.memberName()), false);
					return;
				}
				ProjectService.removeMember(project, memberId);
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.CreateSprintPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null || !canAdmin(project, player)) return;
				ProjectService.addSprint(project, payload.name(), payload.startTime(), payload.endTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.UpdateSprintPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null || !canAdmin(project, player)) return;
				ProjectService.updateSprint(project, payload.sprintId(), payload.name(), payload.startTime(), payload.endTime(), payload.status());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.RequestProjectSyncPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				ProjectViewTracker.addViewer(project.getId(), player);
				sendProjectSync(player, project);
			});
		});
	}

	public static void sendProjectSync(ServerPlayerEntity player, ProjectData project) {
		ServerPlayNetworking.send(player, new MossuraPayloads.ProjectSyncPayload(project.getId(), project.toNbt()));
	}

	private static void markAndSync(MinecraftServer server, ProjectData project) {
		ProjectStore.get(server).markProjectDirty();
		ProjectViewTracker.syncToViewers(server, project);
	}

	private static ProjectData getProject(MinecraftServer server, UUID projectId, ServerPlayerEntity player) {
		ProjectData project = ProjectStore.get(server).getProject(projectId);
		if (project == null) {
			player.sendMessage(Text.literal("Project not found."), false);
		}
		return project;
	}

	private static java.util.List<UUID> resolvePlayerIds(MinecraftServer server, java.util.List<String> assigneeNames) {
		if (assigneeNames == null || assigneeNames.isEmpty()) {
			return new java.util.ArrayList<>();
		}
		java.util.List<UUID> ids = new java.util.ArrayList<>();
		for (String name : assigneeNames) {
			ServerPlayerEntity target = server.getPlayerManager().getPlayer(name);
			if (target != null) {
				ids.add(target.getUuid());
			}
		}
		return ids;
	}

	private static boolean canEdit(ProjectData project, ServerPlayerEntity player) {
		if (project == null || player == null) {
			return false;
		}
		if (project.getOwnerId().equals(player.getUuid())) return true;
		com.notatyler.mossura.project.Member member = project.getMembersMap().get(player.getUuid());
		if (member == null) return false;
		return member.getPermissionLevel() == com.notatyler.mossura.project.Member.PermissionLevel.EDITOR || 
		       member.getPermissionLevel() == com.notatyler.mossura.project.Member.PermissionLevel.ADMIN;
	}

	private static boolean canAdmin(ProjectData project, ServerPlayerEntity player) {
		if (project == null || player == null) {
			return false;
		}
		if (project.getOwnerId().equals(player.getUuid())) return true;
		com.notatyler.mossura.project.Member member = project.getMembersMap().get(player.getUuid());
		return member != null && member.getPermissionLevel() == com.notatyler.mossura.project.Member.PermissionLevel.ADMIN;
	}

	private static boolean canManageMembers(ProjectData project, ServerPlayerEntity player) {
		if (project == null || player == null) {
			return false;
		}
		return project.getOwnerId().equals(player.getUuid());
	}
}
