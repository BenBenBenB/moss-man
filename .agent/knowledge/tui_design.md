# MossMan TUI Design Plan

This document outlines the command structure and the Text User Interface (TUI) components for the MossMan Minecraft mod.

## 1. Command Structure

The root command is `/mossman`.

### Project Management
- `/mossman project list`: Displays all registered projects.
- `/mossman project view <prefix>`: Displays details for a specific project.
- `/mossman project create <prefix> <displayName>`: Creates a new project.
- `/mossman project delete <prefix>` (Admin/Owner only): Deletes a project.
    - *Permissions*: Allowed for Op users, Server Console, and the Project Owner.

> [!NOTE]
> **Localization**: All output will use translation keys (`mossman.tui.*`) to support multiple languages via Minecraft's `lang/*.json` files.

> [!IMPORTANT]
> **Feature Flag**: A server config option (`commands.enabled`) will allow disabling the entire `/mossman` command tree.

### Ticket Management
- `/mossman ticket list <prefix>`: Lists all tickets belonging to a project.
- `/mossman ticket view <key>` (e.g., `MOSS-1`): Displays full ticket details.
- `/mossman ticket create <prefix> <title>`: Initiates ticket creation.
- `/mossman ticket status <key> <newStatus>`: Updates a ticket's status.
- `/mossman ticket comment <key> <message>`: Adds a comment to a ticket.

---

## 2. TUI Output Designs (Mockups)

### 2.1 Project List (`/mossman project list`)
```text
--- MossMan Projects ---
[MOSS] MossMan Mod Development
[TEST] Testing Workbench
------------------------
[Create New Project]
```
*   `[PREFIX]` is clickable: Runs `/mossman project view PREFIX`.
*   `[Create New Project]` is clickable: Suggests `/mossman project create <prefix> <name>`.

### 2.2 Ticket List (`/mossman ticket list MOSS`)
```text
--- Project: MOSS ---
[MOSS-1] Implement TUI commands [IN_PROGRESS]
[MOSS-2] Fix persistence bug [OPEN]
------------------------
[Create Ticket] [Back to Projects]
```
*   `[MOSS-ID]` is clickable: Runs `/mossman ticket view MOSS-ID`.
*   `[Create Ticket]` is clickable: Suggests `/mossman ticket create MOSS `.

### 2.3 Ticket Details (`/mossman ticket view MOSS-1`)
```text
--- MOSS-1: Implement TUI commands ---
Status: IN_PROGRESS | Priority: HIGH
Creator: Player123 | Created: 2026-02-20

Description:
Add interactive /tellraw links for testing the mod's core logic.

Comments:
- Player123: Working on the command tree now.

------------------------
[OPEN] [DONE] [WONT_FIX]
[Add Comment] [Back to List]
```

### 2.4 Project Details (`/mossman project view MOSS`)
```text
--- Project: MossMan Mod Development [MOSS] ---
Owner: Player123
Description: Core mod development project.
Members: 3 | Tickets: 12

[View Tickets] [Rename Project] [Delete Project]
------------------------
[Back to Projects]
```
*   `[View Tickets]`: Runs `/mossman ticket list MOSS`.
*   `[Delete Project]`: Suggests `/mossman project delete MOSS` (if authorized).
*   `[STATUS]` buttons: Run `/mossman ticket status MOSS-1 STATUS`.
*   `[Add Comment]`: Suggests `/mossman ticket comment MOSS-1 `.

---

## 3. Implementation Plan

### Step 1: Command Registration
Use Fabric's `CommandRegistrationCallback` to register the tree.

### Step 2: TUI Utility
Create a `TuiHelper` or `TextComponentBuilder` to generate styled and clickable `Text` objects.

### Step 3: Command Logic
Connect command handlers to Use Cases (`CreateProjectUseCase`, `CreateTicketUseCase`).
