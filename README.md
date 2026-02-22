# MossMan 🌿

MossMan is a modular, native project management mod for Minecraft, designed to bring agile workflows directly into your world. Built for Fabric 1.21.1, it provides a robust platform for tracking tickets, projects, and sprints without external browser dependencies in-game.

## 🚀 Key Features

- **Modular Core**: Designed as a foundational API for extensible project tracking.
- **Native UI**: High-performance, Minecraft-native chat TUI for all project interactions.
- **Agile Toolset**: Full support for Projects, Tickets, Sprints, and Relationships (Needs/Is Needed By).
- **Collaborative Permissions**: Granular roles (Owner, Admin, Editor, Creator, Viewer).
- **Observer System**: Get notified of updates to tickets you care about.
- **RESTful Design**: Built from the ground up to support future external integrations and web APIs.

## 📦 Requirements

- **Minecraft**: 1.21.1
- **Loader**: Fabric
- **Dependencies**: Fabric API

## 💬 In-Game TUI

All interactions happen through clickable chat output — no external browser needed.

### Project List — `/mossman project list [page]`
```
--- MossMan Projects ---
[MOSS] MossMan Mod Development
[TEST] Testing Workbench
« Page 1/2 »
[Create New Project]
```
`[MOSS]` is clickable and runs `/mossman project view MOSS`. Lists are paginated (10 per page). Only projects you have permission to view are shown.

### Project Details — `/mossman project view MOSS`
```
--- Project: [MOSS] ---
[✎] Ticket Prefix: MOSS
[✎] Name: MossMan Mod Development
[✎] Description: Core mod development project.
[✎] Icon Texture: None
[✎] Text Color: None
[✎] External Permission: VIEWER
Members: 2 [View]
Statuses: 3 [View]
Ticket Types: 2 [View]
Relationship Types: 1 [View]
[View Tickets]
```
`[✎]` pencil links appear for editors and suggest the `project update` command pre-filled with the current value. `[View]` links open the respective sub-lists.

### Ticket List — `/mossman ticket list MOSS [page]`
```
--- Tickets: MOSS ---
[MOSS-1] Implement TUI commands [IN_PROGRESS]
[MOSS-2] Fix persistence bug [OPEN]
« Page 1/1 »
[Create Ticket]
```
Each `[MOSS-N]` is clickable and opens the ticket view. Supports optional SNBT filter:

```
/mossman ticket list MOSS {status:"OPEN",priority:"HIGH"}
```

Filterable fields: `status`, `type`, `priority`, `title`.

### Ticket Details — `/mossman ticket view MOSS-1`
```
--- Ticket: MOSS-1 ---
[✎] Title: Implement TUI commands
[✎] Status: IN_PROGRESS
[✎] Description: Add interactive /tellraw links for testing the mod's core logic.
[Edit]
```
`[✎]` pencil links suggest `/mossman ticket update MOSS-1 {field:"value"}` pre-filled. `[Edit]` opens a blank update prompt. Visible to editors only.

### Editing Fields — NBT Patch Syntax

Updates use Minecraft's SNBT (Stringified NBT) compound format:

```
/mossman ticket update MOSS-1 {status:"DONE"}
/mossman ticket update MOSS-1 {title:"New title",priority:"HIGH"}
/mossman project update MOSS {description:"Updated description"}
/mossman project member update MOSS Player123 {permission:"EDITOR"}
```

### Member Management — `/mossman project member ...`
```
--- Members of MossMan Mod Development ---
[Player123] Player123 (OWNER) Bossman
[Player456] Player456 (EDITOR) Redstone Guy
[Add Member]
```
- `member list <prefix> [page]` — paginated, with `[Add Member]` for owners
- `member add <prefix> <player> <permission>` — permissions: `VIEWER`, `CREATOR`, `EDITOR`, `ADMIN`
- `member view <prefix> <player>` — shows title and permission with `[✎]` edit links
- `member update <prefix> <player> <patch>` — patch fields: `title`, `permission`
- `member remove <prefix> <player>`
- `member transfer <prefix> <player>` — transfers project ownership

### Project Schema Management

Editors can manage per-project statuses, ticket types, and relationship types:

- `/mossman project status list/add/view/update <prefix> [name] [patch]`
- `/mossman project ticketType list/add/view/update <prefix> [name] [patch]`
- `/mossman project relationshipType list/add/view/update <prefix> [name] [patch]`

Each `view` command shows `[✎]` edit links for `name` and `textColor` fields.

## 🗺️ Roadmap

### MVP
- TUI autocomplete suggestions for commands
- Ticket assignment — Use Cases and TUI commands
- Notification system — whisper player when added to a ticket's assignees
- In-game GUI screen accessible via `/mossman gui`
- SMP experience features (crafting, etc.)

### Post-MVP
- **Moss Monitor** — placeable block that displays MossMan data; linkable to other monitors via a config item; supports up to 8×8, must form a solid 1-wide rectangle (vertical or horizontal)
- **Moss Master 9000** — multiblock controller block for advanced configuration of linked monitor multiblocks

### Future
- Vanilla JS web app for browser access
- Trello / Jira integrations
- CSV, Excel, and pastebin exports
- String import/export for quick project copying
- Discord bot
- AI project planner

## 📜 Documentation

For more detailed information, see the `.agent/` directory:
- [Architecture](.agent/architecture.md)
- [Data Models](.agent/datamodels.md)
- [Coding Standards](.agent/standards.md)
