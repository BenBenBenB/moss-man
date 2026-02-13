# Coding & Design Standards

## API & Method Standards (RESTful Spirit)
To ensure the core can be easily wrapped by a Web API later, all service methods must follow a resource-oriented pattern:
- **Collection Access**: `list(filter)`, `count()`.
- **Resource Access**: `get(uuid)`, `find(predicate)`.
- **Creation**: `create(dto)` (Returns the created resource).
- **Update**: `patch(uuid, delta)` (Prefer partial updates) or `update(uuid, dto)`.
- **Deletion**: `delete(uuid)`.
- **Actions**: For non-CRUD operations, use `performAction(uuid, actionType, params)`.

## Modularity & Extensibility
- **Core-Only**: Business logic belongs in the API/Core. No UI-specific logic in the service layer.
- **Events**: Use a custom Event system or Fabric's `Event` callbacks to allow other mods to hook into ticket creation, status changes, etc.
- **SPI/Registry**: Provide registries for custom ticket types or status providers.

## Naming Conventions
- **Java Classes**: `PascalCase` (e.g., `MossManScreen`, `CustomButton`).
- **Methods & Variables**: `camelCase`.
- **Assets & Resources**: `snake_case` (required for Minecraft resource locations, e.g., `mossman_background.png`).
- **Packages**: `lowercase` dot-separated (e.g., `com.mossman.client.gui`).
- **Constants**: `SCREAMING_SNAKE_CASE`.
