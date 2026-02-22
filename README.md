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

### Project List — `/mossman project list`
```
--- MossMan Projects ---
[MOSS] MossMan Mod Development
[TEST] Testing Workbench
------------------------
[Create New Project]
```
`[MOSS]` is clickable and runs `/mossman project view MOSS`. `[Create New Project]` suggests the create command.

### Project Details — `/mossman project view MOSS`
```
--- Project: MossMan Mod Development [MOSS] ---
Owner: Player123
Description: Core mod development project.
Members: 3 | Tickets: 12

[View Tickets] [Rename Project] [Delete Project]
------------------------
[Back to Projects]
```

### Ticket List — `/mossman ticket list MOSS`
```
--- Project: MOSS ---
[MOSS-1] Implement TUI commands [IN PROGRESS]
[MOSS-2] Fix persistence bug [OPEN]
------------------------
[Create Ticket] [Back to Projects]
```
Each `[MOSS-ID]` is clickable and opens the ticket details view.

### Ticket Details — `/mossman ticket view MOSS-1`
```
--- MOSS-1: Implement TUI commands ---
Status: IN PROGRESS | Priority: HIGH
Creator: Player123 | Created: 2026-02-20

Description:
Add interactive /tellraw links for testing the mod's core logic.

Comments:
- Player123: Working on the command tree now.

------------------------
[OPEN] [DONE] [WONT_FIX]
[Add Comment] [Back to List]
```
Status buttons run `/mossman ticket status MOSS-1 <STATUS>` directly. `[Add Comment]` suggests the comment command.

## 📜 Documentation

For more detailed information, see the `.agent/` directory:
- [Architecture](.agent/architecture.md)
- [Data Models](.agent/datamodels.md)
- [Coding Standards](.agent/standards.md)
