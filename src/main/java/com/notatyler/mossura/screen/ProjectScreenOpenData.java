package com.notatyler.mossura.screen;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record ProjectScreenOpenData(UUID projectId, NbtCompound projectNbt) {
	public static final PacketCodec<RegistryByteBuf, ProjectScreenOpenData> CODEC = PacketCodec.ofStatic(ProjectScreenOpenData::write, ProjectScreenOpenData::read);

	private static ProjectScreenOpenData read(RegistryByteBuf buf) {
		UUID projectId = buf.readUuid();
		NbtCompound nbt = buf.readNbt();
		return new ProjectScreenOpenData(projectId, nbt == null ? new NbtCompound() : nbt);
	}

	private static void write(RegistryByteBuf buf, ProjectScreenOpenData data) {
		buf.writeUuid(data.projectId());
		buf.writeNbt(data.projectNbt());
	}
}
