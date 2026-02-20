# Data Models

This document defines the core data structures for MossMan, organized by **Clean Architecture** layers.

## Layer Mapping

1. **Domain Entities**: Pure Java objects containing core logic and state.
2. **Persistence Models**: Database-specific representations (OrmLite/NBT) mapped from entities.
3. **DTOs**: Data Transfer Objects used by Adapters (UI, External APIs).

> **Note on Extensibility**: Core models focus strictly on local MossMan data. Integration mods (like Jira/Discord) are responsible for maintaining their own mapping tables or databases to link MossMan entities to external resources.

> **Offline Mode Support**: This mod supports servers with `online-mode=false`. While `UUID` remains the primary identifier for players, internal logic must account for the fact that offline-mode UUIDs are derived from usernames and may change if a player's name changes on a non-authenticated server.

## 1. Project
The top-level container for all tickets and sprints.

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `long` | Unique auto-incrementing database ID. |
| `ticketPrefix` | `String` | Prefix for ticket IDs (e.g., "MOSS"). |
| `name` | `String` | Human-readable project name. |
| `description` | `String` | Optional project summary. |
| `owner` | `UUID` | Minecraft player UUID who owns the project. |
| `iconTexture` | `String` | ResourceLocation/ID of an item or block for the UI icon. |
| `statuses` | `List<Status>` | Custom workflow statuses with branding. |
| `ticketTypes` | `List<TicketType>` | Allowed ticket types with branding. |
| `relationshipTypes` | `List<RelationshipType>` | Definitions for how tickets can relate. |
| `members` | `List<Member>` | List of users with access to the project. |
| `externalUserPermission` | `Enum` | `NONE`, `VIEWER`, `CREATOR`, `EDITOR` |
| `textColor` | `String` | Hex/MC code for displaying the project name or prefix. |

### 1.1 Status / TicketType
| Field | Type | Description |
| :--- | :--- | :--- |
| `name` | `String` | Display name. |
| `textColor` | `String` | Hex/MC code for text. |

### 1.2 RelationshipType 
| Field | Type | Description |
| :--- | :--- | :--- |
| `name` | `String` | Internal name/ID (e.g., "NEEDS"). |
| `sourceToTargetDescription` | `String` | Description of the relation (e.g., "needs"). |
| `targetToSourceDescription` | `String` | Description for the inverse (e.g., "is needed by"). |
| `textColor` | `String` | Hex/MC code for text. |

## 2. Ticket
The primary unit of work.

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `long` | Unique auto-incrementing database ID. |
| `projectId` | `long` | Reference to the parent project ID. |
| `ticketNumber` | `int` | Sequential number within the project (e.g., 123). |
| `title` | `String` | Short summary of the task. |
| `description` | `String` | Detailed explanation. |
| `type` | `String` | Matches the `name` of a Project `TicketType`. |
| `status` | `String` | Matches the `name` of a Project `Status`. |
| `priority` | `Enum` | `LOW`, `MEDIUM`, `HIGH`, `URGENT`. |
| `assignees` | `List<UUID>` | Minecraft player UUIDs assigned to the ticket. |
| `observers` | `List<UUID>` | Player UUIDs to notify of updates. |
| `creator` | `UUID` | Minecraft player UUID who created the ticket. |
| `labels` | `List<String>` | Custom searchable tags. |
| `createdAt` | `long` | Unix timestamp (ms). |
| `updatedAt` | `long` | Unix timestamp (ms). |
| `sprintId` | `long?` | Optional reference to a sprint. |

> **Calculated Key**: The user-friendly key (e.g., `MOSS-123`) is derived as `${Project.ticketPrefix}-${Ticket.ticketNumber}`.

### 2.1 Relationship
| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | `String` | Refers to `name` in Project `RelationshipType`. |
| `sourceTicketId` | `long` | The database ID of the relator ticket. |
| `targetTicketId` | `long` | The database ID of the related ticket. |

## 3. Sprint
A time-boxed period of work.

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `long` | Unique auto-incrementing database ID. |
| `projectId` | `long` | Reference to parent project. |
| `name` | `String` | E.g., "Sprint 1", "February Release". |
| `startTime` | `long` | Unix timestamp for start. |
| `endTime` | `long` | Unix timestamp for end. |
| `status` | `Enum` | `PLANNED`, `ACTIVE`, `COMPLETED`. |

## 4. Member
A user's relationship to a project.

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `long` | Unique database mapping ID. |
| `uuid` | `UUID` | Minecraft player UUID. |
| `name` | `String` | Last known Minecraft username. |
| `permission` | `Enum` | `VIEWER`, `CREATOR`, `EDITOR`, `ADMIN`, `OWNER`. |

## 5. Comment
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `long` | Unique auto-incrementing database ID. |
| `ticketId` | `long` | Reference to parent ticket. |
| `authorId` | `UUID` | Author's Minecraft UUID. |
| `authorName` | `String` | Author's Minecraft username (snapshot at time of comment). |
| `message` | `String` | Content of the comment. |
| `createdAt` | `long` | Unix timestamp. |
