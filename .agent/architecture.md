# System Architecture

## Modular Architecture

MossMan is designed as a **Core-First** project. The `moss-man-core` mod provides the foundational API, data models, and service layer, allowing for extension mods to add integrations (Jira, Web, Discord, etc.).

### 1. API Layer (`com.mossman.api`)
- **Resource-Oriented**: Services and data models are designed to be "RESTful" in spirit.
- **Interfaces**: Define how tickets, projects, and users are managed without coupled implementations.
- **Data Models**: Immutable DTOs/DPOs that are easily serializable for future Web/JSON APIs.

### 2. Core Implementation (`com.mossman.core`)
- **Service Layer**: Business logic for ticket lifecycle, permission checks, and persistence.
- **Persistence**: NBT-based storage with an abstraction layer to allow DB swap-ins later.
- **Command Layer**: A robust set of Fabric commands (`/mossman ...`) exposed to players.

### 3. Native UI (`com.mossman.client`)
- **Purpose**: Fast, native ticket interaction using Minecraft's `Screen` API.
- **Consumer**: Acts as a client of the API layer, ensuring the same logic is used as any future Web UI.


### Java Backend
- **Entrypoints**: (Current TBD in `fabric.mod.json`).
- Handles data persistence, Minecraft world interactions, and bridging to the web UI.

## Data Flow
1. User interacts with minecraft block/item/command to access the GUI.
2. User interacts with GUI components.
3. Java backend processes the action and updates state of NBT data.
4. UI re-renders based on the updated data.
