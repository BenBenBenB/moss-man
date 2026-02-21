package com.mossman.domain.query;

import java.util.Optional;

/**
 * Immutable value object that holds optional filter criteria for ticket queries.
 * Fields are empty when not specified. String fields match case-insensitively.
 */
public record TicketFilter(
        Optional<String> status,
        Optional<String> type,
        Optional<String> priority,
        Optional<String> title
) {
    /** Convenience factory: no filters applied. */
    public static TicketFilter empty() {
        return new TicketFilter(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    /** Parse from an SNBT / JSON-style map already resolved by the caller. */
    public static TicketFilter of(java.util.Map<String, String> map) {
        return new TicketFilter(
                Optional.ofNullable(map.get("status")),
                Optional.ofNullable(map.get("type")),
                Optional.ofNullable(map.get("priority")),
                Optional.ofNullable(map.get("title"))
        );
    }

    /** True if the filter has at least one criterion set. */
    public boolean hasAny() {
        return status.isPresent() || type.isPresent() || priority.isPresent() || title.isPresent();
    }
}
