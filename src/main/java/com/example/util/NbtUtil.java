package com.example.util;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;

public class NbtUtil {
	private NbtUtil() {
	}

	public static void putUuid(NbtCompound nbt, String key, UUID value) {
		if (value != null) {
			nbt.putString(key, value.toString());
		}
	}

	public static UUID getUuid(NbtCompound nbt, String key) {
		String value = nbt.getString(key, "");
		if (value.isEmpty()) {
			return null;
		}
		return UUID.fromString(value);
	}
}
