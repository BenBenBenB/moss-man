package com.notatyler.mossura.web;

import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.project.ProjectService;
import com.notatyler.mossura.project.ProjectStore;
import com.notatyler.mossura.project.TicketUpdate;
import com.notatyler.mossura.tickets.TicketPriority;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public class MossuraWebServer {
    private final MinecraftServer server;
    private HttpServer httpServer;
    private final int port = 8080;

    public MossuraWebServer(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new StaticHandler());
            httpServer.createContext("/api/project", new ProjectApiHandler());
            httpServer.createContext("/api/projects", new ProjectsApiHandler());
            httpServer.createContext("/api/auth", new AuthApiHandler());
            httpServer.createContext("/api/action", new ActionApiHandler());
            httpServer.setExecutor(null);
            httpServer.start();
            System.out.println("Mossura Web Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private String getSessionId(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].equals("sid")) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private String getOrCreateSession(HttpExchange exchange) {
        String sid = getSessionId(exchange);
        if (sid == null) {
            sid = MossuraAuthManager.createSession();
            exchange.getResponseHeaders().add("Set-Cookie", "sid=" + sid + "; Path=/; Max-Age=31536000");
        }
        return sid;
    }

    private UUID getAuthenticatedPlayer(HttpExchange exchange) {
        return MossuraAuthManager.getPlayerUuid(getSessionId(exchange));
    }

    private void sendJson(HttpExchange exchange, int code, Object obj) throws IOException {
        String json = obj.toString();
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    private void sendError(HttpExchange exchange, int code, String msg) throws IOException {
        com.google.gson.JsonObject error = new com.google.gson.JsonObject();
        error.addProperty("error", msg);
        sendJson(exchange, code, error);
    }

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            if (path.startsWith("/")) {
               path = path.substring(1);
            }
            String resourcePath = "assets/mossura/html/ui/" + path;
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                String response = "404 Not Found: " + resourcePath;
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                byte[] content = is.readAllBytes();
                String contentType = "text/html";
                if (path.endsWith(".js")) contentType = "application/javascript";
                else if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".svg")) contentType = "image/svg+xml";
                else if (path.endsWith(".png")) contentType = "image/png";
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.close();
                is.close();
            }
        }
    }

    private class AuthApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/request")) {
                String sid = getOrCreateSession(exchange);
                String token = MossuraAuthManager.requestToken(sid);
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("token", token);
                sendJson(exchange, 200, resp);
            } else if (path.endsWith("/status")) {
                UUID playerUuid = getAuthenticatedPlayer(exchange);
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("authenticated", playerUuid != null);
                if (playerUuid != null) {
                    resp.addProperty("playerUuid", playerUuid.toString());
                    // In a real app we'd look up the name, but for now just ID
                }
                sendJson(exchange, 200, resp);
            } else if (path.endsWith("/logout")) {
                MossuraAuthManager.logout(getSessionId(exchange));
                com.google.gson.JsonObject resp = new com.google.gson.JsonObject();
                resp.addProperty("ok", true);
                sendJson(exchange, 200, resp);
            } else {
                sendError(exchange, 404, "Not found");
            }
        }
    }

    private class ProjectsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            UUID playerUuid = getAuthenticatedPlayer(exchange);
            if (playerUuid == null) {
                sendError(exchange, 401, "Not authenticated");
                return;
            }
            try {
                com.google.gson.JsonArray projects = new com.google.gson.JsonArray();
                for (ProjectData project : ProjectStore.get(server).getProjects()) {
                    if (project.getOwnerId().equals(playerUuid) || project.getMembersMap().containsKey(playerUuid)) {
                        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                        obj.addProperty("id", project.getId().toString());
                        obj.addProperty("name", project.getName());
                        obj.addProperty("role", project.getOwnerId().equals(playerUuid) ? "OWNER" : project.getMembersMap().get(playerUuid).getPermissionLevel().name());
                        projects.add(obj);
                    }
                }
                sendJson(exchange, 200, projects);
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    private class ActionApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            UUID playerUuid = getAuthenticatedPlayer(exchange);
            if (playerUuid == null) {
                sendError(exchange, 401, "Not authenticated");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendError(exchange, 400, "Missing project ID");
                return;
            }
            try {
                UUID projectId = UUID.fromString(parts[3]);
                ProjectData project = ProjectStore.get(server).getProject(projectId);
                if (project == null) {
                    sendError(exchange, 404, "Project not found");
                    return;
                }

                // Parse request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String action = root.get("action").getAsString();
                com.google.gson.JsonObject payload = root.getAsJsonObject("payload");

                String playerName = project.getOwnerId().equals(playerUuid) ? project.getOwnerName() : 
                                  (project.getMembersMap().containsKey(playerUuid) ? project.getMembersMap().get(playerUuid).getName() : "Guest");

                boolean handled = handleAction(project, action, payload, playerUuid, playerName);
                if (handled) {
                    ProjectStore.get(server).markProjectDirty();
                    com.notatyler.mossura.network.ProjectViewTracker.syncToViewers(server, project);
                    sendJson(exchange, 200, new com.google.gson.JsonObject());
                } else {
                    sendError(exchange, 400, "Action failed or unhandled");
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }

        private boolean handleAction(ProjectData project, String action, com.google.gson.JsonObject payload, UUID actorId, String actorName) {
            long time = System.currentTimeMillis();
            if ("create-ticket".equals(action)) {
                String title = payload.get("title").getAsString();
                String desc = payload.has("description") ? payload.get("description").getAsString() : "";
                TicketPriority priority = TicketPriority.MEDIUM;
                try { priority = TicketPriority.valueOf(payload.get("priority").getAsString()); } catch (Exception ignored) {}
                String type = payload.get("type").getAsString();
                String state = payload.get("state").getAsString();
                
                java.util.List<String> assigneeNames = new java.util.ArrayList<>();
                if (payload.has("assigneeNames")) {
                    for (var e : payload.get("assigneeNames").getAsJsonArray()) assigneeNames.add(e.getAsString());
                } else if (payload.has("assigneeName")) {
                    String name = payload.get("assigneeName").getAsString();
                    if (!name.isEmpty()) assigneeNames.add(name);
                }
                
                java.util.List<UUID> assigneeIds = new java.util.ArrayList<>();
                for (String name : assigneeNames) {
                    UUID uuid = project.findMemberIdByName(name);
                    if (uuid != null) assigneeIds.add(uuid);
                }

                com.notatyler.mossura.tickets.Ticket ticket = ProjectService.createTicket(server, project, actorId, actorName, title, desc, priority, type, state, assigneeIds, assigneeNames, time);
                
                if (payload.has("subtasks")) {
                    for (var e : payload.get("subtasks").getAsJsonArray()) {
                        var obj = e.getAsJsonObject();
                        ProjectService.addSubtask(ticket, actorId, actorName, obj.get("name").getAsString(), obj.has("description") ? obj.get("description").getAsString() : "", time);
                    }
                }
                return true;
            }
            if ("update-ticket".equals(action)) {
                UUID ticketId = UUID.fromString(payload.get("ticketId").getAsString());
                com.notatyler.mossura.tickets.Ticket ticket = project.findTicket(ticketId);
                if (ticket == null) return false;
                
                TicketPriority priority = payload.has("priority") ? TicketPriority.valueOf(payload.get("priority").getAsString()) : ticket.getPriority();
                UUID sprintId = payload.has("sprintId") && !payload.get("sprintId").isJsonNull() && !payload.get("sprintId").getAsString().isEmpty() ? UUID.fromString(payload.get("sprintId").getAsString()) : (payload.has("sprintId") ? null : ticket.getSprintId());
                
                java.util.List<String> labels = null;
                if (payload.has("labels")) {
                    labels = new java.util.ArrayList<>();
                    for (var e : payload.get("labels").getAsJsonArray()) labels.add(e.getAsString());
                }
                
                java.util.List<String> assigneeNames = null;
                java.util.List<UUID> assigneeIds = null;
                
                if (payload.has("assigneeNames")) {
                    assigneeNames = new java.util.ArrayList<>();
                    for (var e : payload.get("assigneeNames").getAsJsonArray()) assigneeNames.add(e.getAsString());
                } else if (payload.has("assigneeName")) {
                    assigneeNames = new java.util.ArrayList<>();
                    String name = payload.get("assigneeName").getAsString();
                    if (!name.isEmpty()) assigneeNames.add(name);
                }
                
                if (assigneeNames != null) {
                    assigneeIds = new java.util.ArrayList<>();
                    for (String name : assigneeNames) {
                        UUID uuid = project.findMemberIdByName(name);
                        if (uuid != null) assigneeIds.add(uuid);
                    }
                } else {
                    assigneeNames = ticket.getAssigneeNames();
                    assigneeIds = ticket.getAssigneeIds();
                }

                TicketUpdate update = new TicketUpdate(
                    payload.has("title") ? payload.get("title").getAsString() : ticket.getTitle(),
                    payload.has("description") ? payload.get("description").getAsString() : ticket.getDescription(),
                    priority,
                    payload.has("type") ? payload.get("type").getAsString() : ticket.getType(),
                    payload.has("state") ? payload.get("state").getAsString() : ticket.getState(),
                    assigneeIds,
                    assigneeNames,
                    sprintId,
                    labels
                );
                ProjectService.updateTicket(server, project, ticket, actorId, actorName, update, time);
                return true;
            }
            if ("create-sprint".equals(action)) {
                String name = payload.get("name").getAsString();
                long start = payload.get("startTime").getAsLong();
                long end = payload.get("endTime").getAsLong();
                ProjectService.addSprint(project, name, start, end);
                return true;
            }
            if ("update-sprint".equals(action)) {
                UUID sprintId = UUID.fromString(payload.get("sprintId").getAsString());
                String name = payload.get("name").getAsString();
                long start = payload.get("startTime").getAsLong();
                long end = payload.get("endTime").getAsLong();
                com.notatyler.mossura.project.Sprint.Status status = com.notatyler.mossura.project.Sprint.Status.valueOf(payload.get("status").getAsString());
                ProjectService.updateSprint(project, sprintId, name, start, end, status);
                return true;
            }
            if ("update-project-settings".equals(action)) {
                if (!project.getOwnerId().equals(actorId) && project.getMembersMap().get(actorId).getPermissionLevel() != com.notatyler.mossura.project.Member.PermissionLevel.ADMIN) {
                    return false;
                }
                String name = payload.has("name") ? payload.get("name").getAsString() : null;
                String description = payload.has("description") ? payload.get("description").getAsString() : null;
                String ticketPrefix = payload.has("ticketPrefix") ? payload.get("ticketPrefix").getAsString() : null;
                ProjectService.updateProjectSettings(server, project, name, description, ticketPrefix, null, null, System.currentTimeMillis());
                
                if (payload.has("isPublic")) {
                    ProjectService.updateProjectPublic(project, payload.get("isPublic").getAsBoolean());
                }
                return true;
            }
            return false;
        }
    }

    private class ProjectApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendError(exchange, 400, "Missing project ID");
                return;
            }
            try {
                UUID projectId = UUID.fromString(parts[3]);
                ProjectData project = ProjectStore.get(server).getProject(projectId);
                if (project == null) {
                    sendError(exchange, 404, "Project not found");
                    return;
                }
                
                UUID playerUuid = getAuthenticatedPlayer(exchange);
                if (!project.isPublic()) {
                    if (playerUuid == null || (!project.getOwnerId().equals(playerUuid) && !project.getMembersMap().containsKey(playerUuid))) {
                        sendError(exchange, 403, "Access denied");
                        return;
                    }
                }
                String playerName = "Guest";
                if (playerUuid != null) {
                    // Try to find player name in project members if not online
                    if (project.getMembersMap().containsKey(playerUuid)) {
                        playerName = project.getMembersMap().get(playerUuid).getName();
                    } else if (project.getOwnerId().equals(playerUuid)) {
                        playerName = project.getOwnerName();
                    }
                }

                long nowMillis = System.currentTimeMillis();
                long worldTicks = server.getOverworld().getTime();
                String json = MossuraUiState.projectState(project, playerUuid, playerName, nowMillis, worldTicks).toString();
                byte[] response = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }
}
