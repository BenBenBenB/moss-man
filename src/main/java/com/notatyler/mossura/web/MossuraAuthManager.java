package com.notatyler.mossura.web;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class MossuraAuthManager {
    private static final Map<String, UUID> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, String> PENDING = new ConcurrentHashMap<>(); // token -> sessionId
    private static final Random RANDOM = new Random();

    public static String createSession() {
        return UUID.randomUUID().toString();
    }

    public static String requestToken(String sessionId) {
        // Generate a 6-character alphanumeric token
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        String token = sb.toString();
        PENDING.put(token, sessionId);
        return token;
    }

    public static boolean verify(String token, UUID playerUuid) {
        String sid = PENDING.remove(token.toUpperCase());
        if (sid != null) {
            SESSIONS.put(sid, playerUuid);
            return true;
        }
        return false;
    }

    public static UUID getPlayerUuid(String sessionId) {
        if (sessionId == null) return null;
        return SESSIONS.get(sessionId);
    }

    public static void logout(String sessionId) {
        if (sessionId != null) {
            SESSIONS.remove(sessionId);
        }
    }
}
