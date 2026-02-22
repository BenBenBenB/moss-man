package com.mossman.adapters.tui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mossman.MossManMod;
import com.mossman.domain.auth.PermissionChecker;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Locale;
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
}
