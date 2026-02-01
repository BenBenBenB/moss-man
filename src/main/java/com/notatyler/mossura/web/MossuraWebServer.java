package com.notatyler.mossura.web;

import com.notatyler.mossura.project.MossuraUiState;
import com.notatyler.mossura.project.ProjectData;
import com.notatyler.mossura.project.ProjectStore;
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
                String playerName = "Guest";
                if (playerUuid != null) {
                    // Try to find player name in project members if not online
                    if (project.getMembersMap().containsKey(playerUuid)) {
                        playerName = project.getMembersMap().get(playerUuid).getName();
                    } else if (project.getOwnerId().equals(playerUuid)) {
                        playerName = project.getOwnerName();
                    }
                }

                String json = MossuraUiState.projectState(project, playerUuid, playerName).toString();
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
