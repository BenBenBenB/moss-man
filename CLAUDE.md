# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MossMan is a Fabric 1.21.1 Minecraft mod for in-game agile project management (tickets, sprints, projects). It is designed as a **modular platform** — the core handles all business logic, and extension mods (e.g., `moss-man-jira`) integrate by subscribing to the `DomainEventBus`.

- **Mod ID**: `mossman`
- **Primary Namespace**: `com.mossman`
- **Java 21**, Fabric Loader, OrmLite + SQLite for persistence

## Build Commands

```bash
# Build the mod JAR
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.mossman.infrastructure.persistence.RepositoryTest"
```

Tests use an in-memory SQLite database (`jdbc:sqlite::memory:`) — no Minecraft instance required.

## Architecture

MossMan follows **Clean Architecture** with four layers:

| Layer | Package | Responsibility |
|---|---|---|
| Entities | `com.mossman.domain.entities` | Pure domain models (`Project`, `Ticket`, `Sprint`, etc.), no framework deps |
| Use Cases | `com.mossman.domain.usecases` | Application logic; dispatches Domain Events via `DomainEventBus` |
| Adapters | `com.mossman.adapters` | Brigadier commands (`MossManCommand`→`ProjectCommand`/`TicketCommand`), TUI output |
| Infrastructure | `com.mossman.infrastructure` | OrmLite/SQLite persistence, `SimpleEventBus`, `ConfigManager` |

**Key rule**: inner layers must never depend on outer layers. Use Cases call `ProjectRepository`/`TicketRepository` interfaces (defined in `com.mossman.domain.repositories`); the OrmLite implementations live in infrastructure.

### Entry Point & Wiring

`MossManMod.onInitialize()` is the composition root. It constructs `DatabaseManager`, repositories, use cases, and the `SimpleEventBus`, then registers commands via Fabric's `CommandRegistrationCallback`. Commands can be toggled off via `enableCommands` in `config/mossman.json`.

### Event Flow

1. Player runs a `/mossman` command → Brigadier adapter calls a Use Case.
2. Use Case performs logic, persists via repository, then dispatches a Domain Event (e.g., `TicketCreatedEvent`) to `DomainEventBus`.
3. Listeners (core or extension mods) react to the event for side effects.

### Persistence

`ProjectDb` and `TicketDb` are OrmLite-annotated models that map to/from the clean domain entities. `DatabaseManager` initializes tables on startup. The SQLite file lives at `<game_dir>/mossman.db`.

### TUI

All in-game output is built with `TuiHelper` (clickable `Text` components using `RunCommand`/`SuggestCommand` click events). Commands output styled text, not GUI screens. All player-visible strings must use translation keys (`mossman.tui.*`) resolved via `lang/*.json`.

## Coding Standards

- **RESTful method naming** on services: `list(filter)`, `get(uuid)`, `create(dto)`, `patch(uuid, delta)`, `delete(uuid)`, `performAction(uuid, actionType, params)`.
- **Naming**: `PascalCase` classes, `camelCase` methods/variables, `snake_case` assets/resources, `SCREAMING_SNAKE_CASE` constants.
- Business logic belongs in Use Cases only — no UI or persistence logic in the domain layer.
- Use `FeatureRegistry`/`FeatureProvider` for runtime feature toggles; extension mods register their own flags.

## Agent Documentation

Detailed reference docs live in `.agent/`:
- `.agent/architecture.md` — full architecture diagram and event-driven flow details
- `.agent/datamodels.md` — field-level schema for all domain entities
- `.agent/standards.md` — expanded coding and API standards
- `.agent/knowledge/tui_design.md` — command structure and TUI mockups
