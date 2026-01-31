package com.example.network;

import com.example.Mossura;
import com.example.project.ProjectData;
import com.example.project.ProjectService;
import com.example.project.ProjectStore;
import com.example.project.TicketUpdate;
import com.example.tickets.Subtask;
import com.example.tickets.Ticket;
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
				UUID assigneeId = resolvePlayerId(context.server(), payload.assigneeName());
				ProjectService.createTicket(project, player.getUuid(), player.getName().getString(), payload.title(), payload.description(), payload.priority(), payload.type(), payload.state(), assigneeId, payload.assigneeName(), context.server().getOverworld().getTime());
				markAndSync(context.server(), project);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(MossuraPayloads.UpdateTicketPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				ProjectData project = getProject(context.server(), payload.projectId(), player);
				if (project == null) {
					return;
				}
				Ticket ticket = project.findTicket(payload.ticketId());
				if (ticket == null) {
					player.sendMessage(Text.literal("Ticket not found."), false);
					return;
				}
				UUID assigneeId = resolvePlayerId(context.server(), payload.assigneeName());
				TicketUpdate update = new TicketUpdate(payload.title(), payload.description(), payload.priority(), payload.type(), payload.state(), assigneeId, payload.assigneeName());
				ProjectService.updateTicket(project, ticket, player.getUuid(), player.getName().getString(), update, context.server().getOverworld().getTime());
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
				ProjectService.updateProjectSettings(project, payload.name(), payload.statuses(), payload.ticketTypes());
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
				ServerPlayerEntity target = context.server().getPlayerManager().getPlayer(payload.memberName());
				if (target == null) {
					player.sendMessage(Text.literal("Player not found: " + payload.memberName()), false);
					return;
				}
				ProjectService.addMember(project, target.getUuid(), target.getName().getString());
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
				UUID memberId = project.findMemberIdByName(payload.memberName());
				if (memberId == null) {
					player.sendMessage(Text.literal("Member not found: " + payload.memberName()), false);
					return;
				}
				ProjectService.removeMember(project, memberId);
				markAndSync(context.server(), project);
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

	private static UUID resolvePlayerId(MinecraftServer server, String assigneeName) {
		if (assigneeName == null || assigneeName.isBlank()) {
			return null;
		}
		ServerPlayerEntity target = server.getPlayerManager().getPlayer(assigneeName);
		return target == null ? null : target.getUuid();
	}
}
