package com.notatyler.mossura.network;

import com.notatyler.mossura.Mossura;
import com.notatyler.mossura.tickets.TicketPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class MossuraPayloads {
	private MossuraPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(CreateTicketPayload.ID, CreateTicketPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateTicketPayload.ID, UpdateTicketPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DeleteTicketPayload.ID, DeleteTicketPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateSubtaskPayload.ID, UpdateSubtaskPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SetSubtaskCompletePayload.ID, SetSubtaskCompletePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AddTicketCommentPayload.ID, AddTicketCommentPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AddSubtaskPayload.ID, AddSubtaskPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AddSubtaskCommentPayload.ID, AddSubtaskCommentPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateProjectSettingsPayload.ID, UpdateProjectSettingsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AddProjectMemberPayload.ID, AddProjectMemberPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoveProjectMemberPayload.ID, RemoveProjectMemberPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CreateSprintPayload.ID, CreateSprintPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateSprintPayload.ID, UpdateSprintPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestProjectSyncPayload.ID, RequestProjectSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ProjectSyncPayload.ID, ProjectSyncPayload.CODEC);
	}

	public record ProjectSyncPayload(UUID projectId, NbtCompound projectNbt) implements CustomPayload {
		public static final Id<ProjectSyncPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "project_sync"));
		public static final PacketCodec<RegistryByteBuf, ProjectSyncPayload> CODEC = PacketCodec.ofStatic(ProjectSyncPayload::write, ProjectSyncPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static ProjectSyncPayload read(RegistryByteBuf buf) {
			UUID projectId = buf.readUuid();
			NbtCompound nbt = buf.readNbt();
			return new ProjectSyncPayload(projectId, nbt == null ? new NbtCompound() : nbt);
		}

		private static void write(RegistryByteBuf buf, ProjectSyncPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeNbt(payload.projectNbt);
		}
	}

	public record CreateTicketPayload(UUID projectId, String title, String description, String type, String state, TicketPriority priority, List<String> assigneeNames, UUID sprintId, List<String> labels) implements CustomPayload {
		public static final Id<CreateTicketPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "create_ticket"));
		public static final PacketCodec<RegistryByteBuf, CreateTicketPayload> CODEC = PacketCodec.ofStatic(CreateTicketPayload::write, CreateTicketPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static CreateTicketPayload read(RegistryByteBuf buf) {
			UUID projectId = buf.readUuid();
			String title = buf.readString(256);
			String description = buf.readString(2048);
			String type = buf.readString(64);
			String state = buf.readString(64);
			TicketPriority priority = TicketPriority.valueOf(buf.readString(32));
			List<String> assigneeNames = readStringList(buf, 64);
			boolean hasSprint = buf.readBoolean();
			UUID sprintId = hasSprint ? buf.readUuid() : null;
			List<String> labels = readStringList(buf, 64);
			return new CreateTicketPayload(projectId, title, description, type, state, priority, assigneeNames, sprintId, labels);
		}

		private static void write(RegistryByteBuf buf, CreateTicketPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeString(payload.title, 256);
			buf.writeString(payload.description, 2048);
			buf.writeString(payload.type, 64);
			buf.writeString(payload.state, 64);
			buf.writeString(payload.priority.name(), 32);
			writeStringList(buf, payload.assigneeNames);
			if (payload.sprintId != null) {
				buf.writeBoolean(true);
				buf.writeUuid(payload.sprintId);
			} else {
				buf.writeBoolean(false);
			}
			writeStringList(buf, payload.labels);
		}
	}

	public record UpdateTicketPayload(UUID projectId, UUID ticketId, String title, String description, String type, String state, TicketPriority priority, List<String> assigneeNames, UUID sprintId, List<String> labels) implements CustomPayload {
		public static final Id<UpdateTicketPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "update_ticket"));
		public static final PacketCodec<RegistryByteBuf, UpdateTicketPayload> CODEC = PacketCodec.ofStatic(UpdateTicketPayload::write, UpdateTicketPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static UpdateTicketPayload read(RegistryByteBuf buf) {
			UUID projectId = buf.readUuid();
			UUID ticketId = buf.readUuid();
			String title = buf.readString(256);
			String description = buf.readString(2048);
			String type = buf.readString(64);
			String state = buf.readString(64);
			TicketPriority priority = TicketPriority.valueOf(buf.readString(32));
			List<String> assigneeNames = readStringList(buf, 64);
			boolean hasSprint = buf.readBoolean();
			UUID sprintId = hasSprint ? buf.readUuid() : null;
			List<String> labels = readStringList(buf, 64);
			return new UpdateTicketPayload(projectId, ticketId, title, description, type, state, priority, assigneeNames, sprintId, labels);
		}

		private static void write(RegistryByteBuf buf, UpdateTicketPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
			buf.writeString(payload.title, 256);
			buf.writeString(payload.description, 2048);
			buf.writeString(payload.type, 64);
			buf.writeString(payload.state, 64);
			buf.writeString(payload.priority.name(), 32);
			writeStringList(buf, payload.assigneeNames);
			if (payload.sprintId != null) {
				buf.writeBoolean(true);
				buf.writeUuid(payload.sprintId);
			} else {
				buf.writeBoolean(false);
			}
			writeStringList(buf, payload.labels);
		}
	}

	public record DeleteTicketPayload(UUID projectId, UUID ticketId) implements CustomPayload {
		public static final Id<DeleteTicketPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "delete_ticket"));
		public static final PacketCodec<RegistryByteBuf, DeleteTicketPayload> CODEC = PacketCodec.ofStatic(DeleteTicketPayload::write, DeleteTicketPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static DeleteTicketPayload read(RegistryByteBuf buf) {
			return new DeleteTicketPayload(buf.readUuid(), buf.readUuid());
		}

		private static void write(RegistryByteBuf buf, DeleteTicketPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
		}
	}

	public record UpdateSubtaskPayload(UUID projectId, UUID ticketId, UUID subtaskId, String name, String description) implements CustomPayload {
		public static final Id<UpdateSubtaskPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "update_subtask"));
		public static final PacketCodec<RegistryByteBuf, UpdateSubtaskPayload> CODEC = PacketCodec.ofStatic(UpdateSubtaskPayload::write, UpdateSubtaskPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static UpdateSubtaskPayload read(RegistryByteBuf buf) {
			UUID projectId = buf.readUuid();
			UUID ticketId = buf.readUuid();
			UUID subtaskId = buf.readUuid();
			String name = buf.readString(256);
			String description = buf.readString(2048);
			return new UpdateSubtaskPayload(projectId, ticketId, subtaskId, name, description);
		}

		private static void write(RegistryByteBuf buf, UpdateSubtaskPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
			buf.writeUuid(payload.subtaskId);
			buf.writeString(payload.name, 256);
			buf.writeString(payload.description, 2048);
		}
	}

	public record SetSubtaskCompletePayload(UUID projectId, UUID ticketId, UUID subtaskId, boolean completed) implements CustomPayload {
		public static final Id<SetSubtaskCompletePayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "set_subtask_complete"));
		public static final PacketCodec<RegistryByteBuf, SetSubtaskCompletePayload> CODEC = PacketCodec.ofStatic(SetSubtaskCompletePayload::write, SetSubtaskCompletePayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static SetSubtaskCompletePayload read(RegistryByteBuf buf) {
			return new SetSubtaskCompletePayload(buf.readUuid(), buf.readUuid(), buf.readUuid(), buf.readBoolean());
		}

		private static void write(RegistryByteBuf buf, SetSubtaskCompletePayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
			buf.writeUuid(payload.subtaskId);
			buf.writeBoolean(payload.completed);
		}
	}

	public record AddTicketCommentPayload(UUID projectId, UUID ticketId, String message) implements CustomPayload {
		public static final Id<AddTicketCommentPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "add_ticket_comment"));
		public static final PacketCodec<RegistryByteBuf, AddTicketCommentPayload> CODEC = PacketCodec.ofStatic(AddTicketCommentPayload::write, AddTicketCommentPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static AddTicketCommentPayload read(RegistryByteBuf buf) {
			return new AddTicketCommentPayload(buf.readUuid(), buf.readUuid(), buf.readString(2048));
		}

		private static void write(RegistryByteBuf buf, AddTicketCommentPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
			buf.writeString(payload.message, 2048);
		}
	}

	public record AddSubtaskPayload(UUID projectId, UUID ticketId, String name, String description) implements CustomPayload {
		public static final Id<AddSubtaskPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "add_subtask"));
		public static final PacketCodec<RegistryByteBuf, AddSubtaskPayload> CODEC = PacketCodec.ofStatic(AddSubtaskPayload::write, AddSubtaskPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static AddSubtaskPayload read(RegistryByteBuf buf) {
			return new AddSubtaskPayload(buf.readUuid(), buf.readUuid(), buf.readString(256), buf.readString(2048));
		}

		private static void write(RegistryByteBuf buf, AddSubtaskPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
			buf.writeString(payload.name, 256);
			buf.writeString(payload.description, 2048);
		}
	}

	public record AddSubtaskCommentPayload(UUID projectId, UUID ticketId, UUID subtaskId, String message) implements CustomPayload {
		public static final Id<AddSubtaskCommentPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "add_subtask_comment"));
		public static final PacketCodec<RegistryByteBuf, AddSubtaskCommentPayload> CODEC = PacketCodec.ofStatic(AddSubtaskCommentPayload::write, AddSubtaskCommentPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static AddSubtaskCommentPayload read(RegistryByteBuf buf) {
			return new AddSubtaskCommentPayload(buf.readUuid(), buf.readUuid(), buf.readUuid(), buf.readString(2048));
		}

		private static void write(RegistryByteBuf buf, AddSubtaskCommentPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.ticketId);
			buf.writeUuid(payload.subtaskId);
			buf.writeString(payload.message, 2048);
		}
	}

	public record UpdateProjectSettingsPayload(UUID projectId, String name, String description, String ticketPrefix, List<String> statuses, List<String> ticketTypes) implements CustomPayload {
		public static final Id<UpdateProjectSettingsPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "update_project_settings"));
		public static final PacketCodec<RegistryByteBuf, UpdateProjectSettingsPayload> CODEC = PacketCodec.ofStatic(UpdateProjectSettingsPayload::write, UpdateProjectSettingsPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static UpdateProjectSettingsPayload read(RegistryByteBuf buf) {
			UUID projectId = buf.readUuid();
			String name = buf.readString(128);
			String description = buf.readString(2048);
			String ticketPrefix = buf.readString(16);
			List<String> statuses = readStringList(buf, 64);
			List<String> types = readStringList(buf, 64);
			return new UpdateProjectSettingsPayload(projectId, name, description, ticketPrefix, statuses, types);
		}

		private static void write(RegistryByteBuf buf, UpdateProjectSettingsPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeString(payload.name, 128);
			buf.writeString(payload.description, 2048);
			buf.writeString(payload.ticketPrefix, 16);
			writeStringList(buf, payload.statuses);
			writeStringList(buf, payload.ticketTypes);
		}
	}

	public record AddProjectMemberPayload(UUID projectId, String memberName, com.notatyler.mossura.project.Member.PermissionLevel level) implements CustomPayload {
		public static final Id<AddProjectMemberPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "add_project_member"));
		public static final PacketCodec<RegistryByteBuf, AddProjectMemberPayload> CODEC = PacketCodec.ofStatic(AddProjectMemberPayload::write, AddProjectMemberPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static AddProjectMemberPayload read(RegistryByteBuf buf) {
			return new AddProjectMemberPayload(buf.readUuid(), buf.readString(64), com.notatyler.mossura.project.Member.PermissionLevel.valueOf(buf.readString(32)));
		}

		private static void write(RegistryByteBuf buf, AddProjectMemberPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeString(payload.memberName, 64);
			buf.writeString(payload.level.name(), 32);
		}
	}

	public record CreateSprintPayload(UUID projectId, String name, long startTime, long endTime) implements CustomPayload {
		public static final Id<CreateSprintPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "create_sprint"));
		public static final PacketCodec<RegistryByteBuf, CreateSprintPayload> CODEC = PacketCodec.ofStatic(CreateSprintPayload::write, CreateSprintPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static CreateSprintPayload read(RegistryByteBuf buf) {
			return new CreateSprintPayload(buf.readUuid(), buf.readString(128), buf.readLong(), buf.readLong());
		}

		private static void write(RegistryByteBuf buf, CreateSprintPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeString(payload.name, 128);
			buf.writeLong(payload.startTime);
			buf.writeLong(payload.endTime);
		}
	}

	public record UpdateSprintPayload(UUID projectId, UUID sprintId, String name, long startTime, long endTime, com.notatyler.mossura.project.Sprint.Status status) implements CustomPayload {
		public static final Id<UpdateSprintPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "update_sprint"));
		public static final PacketCodec<RegistryByteBuf, UpdateSprintPayload> CODEC = PacketCodec.ofStatic(UpdateSprintPayload::write, UpdateSprintPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static UpdateSprintPayload read(RegistryByteBuf buf) {
			return new UpdateSprintPayload(buf.readUuid(), buf.readUuid(), buf.readString(128), buf.readLong(), buf.readLong(), com.notatyler.mossura.project.Sprint.Status.valueOf(buf.readString(32)));
		}

		private static void write(RegistryByteBuf buf, UpdateSprintPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeUuid(payload.sprintId);
			buf.writeString(payload.name, 128);
			buf.writeLong(payload.startTime);
			buf.writeLong(payload.endTime);
			buf.writeString(payload.status.name(), 32);
		}
	}

	public record RemoveProjectMemberPayload(UUID projectId, String memberName) implements CustomPayload {
		public static final Id<RemoveProjectMemberPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "remove_project_member"));
		public static final PacketCodec<RegistryByteBuf, RemoveProjectMemberPayload> CODEC = PacketCodec.ofStatic(RemoveProjectMemberPayload::write, RemoveProjectMemberPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static RemoveProjectMemberPayload read(RegistryByteBuf buf) {
			return new RemoveProjectMemberPayload(buf.readUuid(), buf.readString(64));
		}

		private static void write(RegistryByteBuf buf, RemoveProjectMemberPayload payload) {
			buf.writeUuid(payload.projectId);
			buf.writeString(payload.memberName, 64);
		}
	}

	public record RequestProjectSyncPayload(UUID projectId) implements CustomPayload {
		public static final Id<RequestProjectSyncPayload> ID = new Id<>(Identifier.of(Mossura.MOD_ID, "request_project_sync"));
		public static final PacketCodec<RegistryByteBuf, RequestProjectSyncPayload> CODEC = PacketCodec.ofStatic(RequestProjectSyncPayload::write, RequestProjectSyncPayload::read);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

		private static RequestProjectSyncPayload read(RegistryByteBuf buf) {
			return new RequestProjectSyncPayload(buf.readUuid());
		}

		private static void write(RegistryByteBuf buf, RequestProjectSyncPayload payload) {
			buf.writeUuid(payload.projectId);
		}
	}

	private static void writeStringList(RegistryByteBuf buf, List<String> values) {
		buf.writeInt(values.size());
		for (String value : values) {
			buf.writeString(value, 64);
		}
	}

	private static List<String> readStringList(RegistryByteBuf buf, int maxLength) {
		int size = buf.readInt();
		List<String> values = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			values.add(buf.readString(maxLength));
		}
		return values;
	}
}
