package com.mossman.adapters.tui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mossman.MossManMod;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Project;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SuggestionHelper {

    private SuggestionHelper() {}

    /** Suggests ticket prefixes for all projects the caller can view. */
    public static CompletableFuture<Suggestions> suggestVisiblePrefixes(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        var prefixes = MossManMod.getProjectRepository()
                .findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> PermissionChecker.canView(p, ctx.getSource()))
                .map(p -> p.getTicketPrefix());
        return suggest(builder, prefixes);
    }

    /** Suggests all ticket keys (e.g. MOSS-1) across all projects the caller can view. */
    public static CompletableFuture<Suggestions> suggestTicketKeys(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        var source = ctx.getSource();
        var keys = MossManMod.getProjectRepository()
                .findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> PermissionChecker.canView(p, source))
                .flatMap(p -> MossManMod.getTicketRepository()
                        .findByProjectId(p.getId(), 0, Integer.MAX_VALUE).stream()
                        .map(t -> t.getUserFriendlyKey(p.getTicketPrefix())));
        return suggest(builder, keys);
    }

    /**
     * Returns a provider that suggests status names for the project identified
     * by the already-parsed {@code prefixArgName} argument in the context.
     */
    public static SuggestionProvider<ServerCommandSource> suggestStatusNames(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return suggest(builder, project.getStatuses().stream().map(s -> s.name()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a provider that suggests ticket type names for the project identified
     * by the already-parsed {@code prefixArgName} argument in the context.
     */
    public static SuggestionProvider<ServerCommandSource> suggestTicketTypeNames(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return suggest(builder, project.getTicketTypes().stream().map(t -> t.name()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a provider that suggests relationship type names for the project identified
     * by the already-parsed {@code prefixArgName} argument in the context.
     */
    public static SuggestionProvider<ServerCommandSource> suggestRelationshipTypeNames(String prefixArgName) {
        return (ctx, builder) -> {
            try {
                String prefix = StringArgumentType.getString(ctx, prefixArgName);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                return suggest(builder, project.getRelationshipTypes().stream().map(r -> r.name()));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /**
     * Returns a provider that suggests the current assignee names for a ticket,
     * identified by the already-parsed {@code keyArgName} argument (e.g. "MOSS-1").
     * Useful for the {@code ticket unassign} command.
     */
    public static SuggestionProvider<ServerCommandSource> suggestTicketAssignees(String keyArgName) {
        return (ctx, builder) -> {
            try {
                String key = StringArgumentType.getString(ctx, keyArgName);
                String[] parts = key.split("-");
                if (parts.length < 2) return builder.buildFuture();
                String prefix = parts[0];
                int number = Integer.parseInt(parts[1]);
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                if (project == null) return builder.buildFuture();
                var ticket = MossManMod.getTicketRepository()
                        .findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                        .filter(t -> t.getTicketNumber() == number)
                        .findFirst().orElse(null);
                if (ticket == null) return builder.buildFuture();
                var server = ctx.getSource().getServer();
                var names = ticket.getAssignees().stream()
                        .map(uuid -> {
                            var m = project.getMembers().stream()
                                    .filter(mm -> mm.uuid().equals(uuid)).findFirst();
                            if (m.isPresent()) return m.get().username();
                            var online = server.getPlayerManager().getPlayer(uuid);
                            return online != null ? online.getName().getString() : uuid.toString();
                        });
                return suggest(builder, names);
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /** Suggests the assignable permission values (excludes FORBID and OWNER). */
    public static CompletableFuture<Suggestions> suggestAssignablePermissions(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        return suggest(builder, List.of("VIEWER", "CREATOR", "EDITOR", "ADMIN").stream());
    }

    /** Filters {@code candidates} by the remaining input and adds matching entries to the builder. */
    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Stream<String> candidates) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        candidates
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    // ==================== SNBT Patch Autocomplete ====================

    private enum PatchParseState { BEFORE_OPEN, IN_KEY, AFTER_COLON, IN_VALUE, AFTER_VALUE, DONE }

    private record PatchCursor(
            String completedPrefix,
            String currentToken,
            PatchParseState state,
            String currentKey,
            Set<String> seenKeys) {}

    /**
     * Walks the partial SNBT string character-by-character to determine the
     * autocomplete cursor state (what field/value position the caret is at).
     */
    private static PatchCursor parsePatchCursor(String input) {
        if (input.isEmpty()) {
            return new PatchCursor("", "", PatchParseState.BEFORE_OPEN, null, new LinkedHashSet<>());
        }
        PatchParseState state = PatchParseState.BEFORE_OPEN;
        StringBuilder prefix = new StringBuilder();
        StringBuilder token = new StringBuilder();
        String currentKey = null;
        Set<String> seenKeys = new LinkedHashSet<>();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (state) {
                case BEFORE_OPEN -> {
                    if (c == '{') { prefix.append(c); state = PatchParseState.IN_KEY; }
                }
                case IN_KEY -> {
                    if (c == ':') {
                        currentKey = token.toString();
                        seenKeys.add(currentKey);
                        prefix.append(token).append(':');
                        token.setLength(0);
                        state = PatchParseState.AFTER_COLON;
                    } else if (c == ',') {
                        prefix.append(token).append(',');
                        token.setLength(0);
                    } else if (c == '}') {
                        state = PatchParseState.DONE;
                    } else {
                        token.append(c);
                    }
                }
                case AFTER_COLON -> {
                    if (c == '"') { prefix.append('"'); state = PatchParseState.IN_VALUE; }
                }
                case IN_VALUE -> {
                    if (c == '"' && (i == 0 || input.charAt(i - 1) != '\\')) {
                        prefix.append(token).append('"');
                        token.setLength(0);
                        state = PatchParseState.AFTER_VALUE;
                    } else {
                        token.append(c);
                    }
                }
                case AFTER_VALUE -> {
                    if (c == ',') { prefix.append(','); state = PatchParseState.IN_KEY; }
                    else if (c == '}') { state = PatchParseState.DONE; }
                }
                default -> {}
            }
        }
        return new PatchCursor(prefix.toString(), token.toString(), state, currentKey, seenKeys);
    }

    private static CompletableFuture<Suggestions> generatePatchSuggestions(
            SuggestionsBuilder builder, PatchCursor cursor, Map<String, List<String>> fields) {
        SuggestionsBuilder ob = builder.createOffset(builder.getStart() + cursor.completedPrefix().length());
        return switch (cursor.state()) {
            case BEFORE_OPEN -> suggest(ob, Stream.of("{"));
            case IN_KEY -> suggest(ob, fields.keySet().stream()
                    .filter(f -> !cursor.seenKeys().contains(f))
                    .map(f -> f + ":"));
            case AFTER_COLON -> suggest(ob, Stream.of("\""));
            case IN_VALUE -> {
                List<String> values = cursor.currentKey() != null ? fields.get(cursor.currentKey()) : null;
                if (values != null) yield suggest(ob, values.stream().map(v -> v + "\""));
                yield suggest(ob, Stream.of("\""));
            }
            case AFTER_VALUE -> suggest(ob, Stream.of("}", ","));
            default -> ob.buildFuture();
        };
    }

    // --- Static field maps for each entity type ---

    private static final Map<String, List<String>> PROJECT_FIELDS;
    private static final Map<String, List<String>> MEMBER_FIELDS;
    private static final Map<String, List<String>> STATUS_FIELDS;
    private static final Map<String, List<String>> TICKET_TYPE_FIELDS;
    private static final Map<String, List<String>> RELATIONSHIP_TYPE_FIELDS;

    static {
        Map<String, List<String>> m;

        m = new LinkedHashMap<>();
        m.put("name", null);
        m.put("description", null);
        m.put("ticketPrefix", null);
        m.put("iconTexture", null);
        m.put("textColor", null);
        m.put("externalUserPermission", List.of("FORBID", "VIEWER", "CREATOR", "EDITOR", "ADMIN"));
        PROJECT_FIELDS = Collections.unmodifiableMap(m);

        m = new LinkedHashMap<>();
        m.put("title", null);
        m.put("permission", List.of("VIEWER", "CREATOR", "EDITOR", "ADMIN"));
        MEMBER_FIELDS = Collections.unmodifiableMap(m);

        m = new LinkedHashMap<>();
        m.put("name", null);
        m.put("textColor", null);
        STATUS_FIELDS = Collections.unmodifiableMap(m);

        TICKET_TYPE_FIELDS = STATUS_FIELDS; // identical schema

        m = new LinkedHashMap<>();
        m.put("name", null);
        m.put("textColor", null);
        m.put("sourceToTargetDescription", null);
        m.put("targetToSourceDescription", null);
        RELATIONSHIP_TYPE_FIELDS = Collections.unmodifiableMap(m);
    }

    private static Map<String, List<String>> buildTicketFields(Project project) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("title", null);
        m.put("description", null);
        m.put("priority", List.of("LOW", "MEDIUM", "HIGH", "URGENT"));
        if (project != null) {
            m.put("status", project.getStatuses().stream().map(s -> s.name()).toList());
            m.put("type", project.getTicketTypes().stream().map(t -> t.name()).toList());
        } else {
            m.put("status", null);
            m.put("type", null);
        }
        return m;
    }

    /** Returns a patch suggestion provider for {@code ticket update}; reads project via {@code keyArgName}. */
    public static SuggestionProvider<ServerCommandSource> suggestTicketPatch(String keyArgName) {
        return (ctx, builder) -> {
            try {
                String key = StringArgumentType.getString(ctx, keyArgName);
                String prefix = key.split("-")[0];
                var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                        .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                        .findFirst().orElse(null);
                return generatePatchSuggestions(builder, parsePatchCursor(builder.getRemaining()),
                        buildTicketFields(project));
            } catch (Exception e) {
                return builder.buildFuture();
            }
        };
    }

    /** Returns a patch suggestion provider for {@code project update}. */
    public static SuggestionProvider<ServerCommandSource> suggestProjectPatch() {
        return (ctx, builder) -> generatePatchSuggestions(
                builder, parsePatchCursor(builder.getRemaining()), PROJECT_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project member update}. */
    public static SuggestionProvider<ServerCommandSource> suggestMemberPatch() {
        return (ctx, builder) -> generatePatchSuggestions(
                builder, parsePatchCursor(builder.getRemaining()), MEMBER_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project status update}. */
    public static SuggestionProvider<ServerCommandSource> suggestStatusPatch() {
        return (ctx, builder) -> generatePatchSuggestions(
                builder, parsePatchCursor(builder.getRemaining()), STATUS_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project ticketType update}. */
    public static SuggestionProvider<ServerCommandSource> suggestTicketTypePatch() {
        return (ctx, builder) -> generatePatchSuggestions(
                builder, parsePatchCursor(builder.getRemaining()), TICKET_TYPE_FIELDS);
    }

    /** Returns a patch suggestion provider for {@code project relationshipType update}. */
    public static SuggestionProvider<ServerCommandSource> suggestRelationshipTypePatch() {
        return (ctx, builder) -> generatePatchSuggestions(
                builder, parsePatchCursor(builder.getRemaining()), RELATIONSHIP_TYPE_FIELDS);
    }
}
