package net.clusters_prj.replugin;

import org.bukkit.plugin.java.JavaPlugin;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourcePackServer {

    private final JavaPlugin plugin;
    private final int port;
    private final File packsFolder;
    private final String servePath;
    private HttpServer httpServer;

    public ResourcePackServer(JavaPlugin plugin, int port, File packsFolder, String servePath) {
        this.plugin = plugin;
        this.port = port;
        this.packsFolder = packsFolder;
        this.servePath = servePath.endsWith("/") ? servePath : servePath + "/";
    }

    /**
     * HTTPサーバーを開始
     */
    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            httpServer.createContext(servePath, new ResourcePackHandler());
            httpServer.setExecutor(null);
            httpServer.start();
            
            plugin.getLogger().info("Resource Pack Server started on port " + port + " at path " + servePath);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start Resource Pack Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * HTTPサーバーを停止
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            plugin.getLogger().info("Resource Pack Server stopped.");
        }
    }

    /**
     * リソースパック用HTTPハンドラー
     */
    private class ResourcePackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // パス正規化（セキュリティ対策）
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // ディレクトリトラバーサル対策
            if (path.contains("..") || path.contains("~")) {
                sendError(exchange, 403, "Forbidden");
                return;
            }
            
            File file = new File(packsFolder, path);
            
            // ファイルが packs フォルダ内か確認
            try {
                if (!file.getCanonicalPath().startsWith(packsFolder.getCanonicalPath())) {
                    sendError(exchange, 403, "Forbidden");
                    return;
                }
            } catch (IOException e) {
                sendError(exchange, 500, "Internal Server Error");
                return;
            }
            
            // .zip ファイルのみ配信
            if (!file.exists() || !file.isFile() || !file.getName().endsWith(".zip")) {
                sendError(exchange, 404, "Not Found");
                return;
            }
            
            // ファイルを配信
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] fileBytes = fis.readAllBytes();
                
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileBytes.length));
                exchange.sendResponseHeaders(200, fileBytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileBytes);
                }
                
                plugin.getLogger().info("Served resource pack: " + file.getName());
            } catch (IOException e) {
                sendError(exchange, 500, "Internal Server Error");
                plugin.getLogger().severe("Error serving file: " + e.getMessage());
            }
        }

        private void sendError(HttpExchange exchange, int code, String message) throws IOException {
            String response = code + " " + message;
            exchange.sendResponseHeaders(code, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
