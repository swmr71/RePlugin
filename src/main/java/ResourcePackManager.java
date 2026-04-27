package net.clusters_prj.replugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public class ResourcePackManager {

    private final JavaPlugin plugin;
    private final List<ResourcePackInfo> packs = Collections.synchronizedList(new ArrayList<>());
    private final File packsFolder;

    public ResourcePackManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.packsFolder = new File(plugin.getDataFolder(), "packs");
        
        if (!packsFolder.exists()) {
            packsFolder.mkdirs();
        }
    }

    /**
     * 複数GitHub Releasesからリソースパックをダウンロード
     */
    public void downloadFromGithub(ConfigurationSection githubConfig) {
        if (githubConfig == null || !githubConfig.isList("sources")) {
            plugin.getLogger().warning("No GitHub sources configured in config.yml");
            return;
        }
        
        List<Map<?, ?>> sources = githubConfig.getMapList("sources");
        for (Map<?, ?> source : sources) {
            String owner = (String) source.get("owner");
            String repo = (String) source.get("repo");
            String tag = (String) source.get("tag");
            String filename = (String) source.get("filename");
            
            if (owner != null && repo != null && tag != null && filename != null) {
                downloadSingleRelease(owner, repo, tag, filename);
            } else {
                plugin.getLogger().warning("Incomplete GitHub source config: owner=" + owner + 
                    ", repo=" + repo + ", tag=" + tag + ", filename=" + filename);
            }
        }
    }

    /**
     * 単一のGitHub Releaseをダウンロード
     */
    private void downloadSingleRelease(String owner, String repo, String tag, String filename) {
        new Thread(() -> {
            try {
                // 直接URLを構築: https://github.com/{owner}/{repo}/releases/download/{tag}/{filename}
                String downloadUrl = "https://github.com/" + owner + "/" + repo + 
                    "/releases/download/" + tag + "/" + filename;
                String githubUrl = downloadUrl;
                
                plugin.getLogger().info("Downloading from GitHub: " + downloadUrl);
                downloadPack(filename, downloadUrl, githubUrl);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to download from GitHub: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 個別パックをダウンロード
     */
    private void downloadPack(String filename, String downloadUrl, String githubUrl) {
        try {
            File packFile = new File(packsFolder, filename);
            
            // 既に存在する場合はスキップ
            if (packFile.exists()) {
                plugin.getLogger().info("Pack already exists: " + filename);
                registerGithubPack(packFile, githubUrl);
                return;
            }
            
            // ダウンロード
            URLConnection connection = new URL(downloadUrl).openConnection();
            connection.addRequestProperty("User-Agent", "RePlugin/1.0");
            
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(packFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            plugin.getLogger().info("Downloaded: " + filename);
            registerGithubPack(packFile, githubUrl);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to download pack: " + filename + " - " + e.getMessage());
        }
    }

    /**
     * GitHub Releaseからダウンロードしたパックを登録（GitHub URLそのままを使用）
     */
    private void registerGithubPack(File packFile, String githubUrl) {
        try {
            String hash = calculateSha1(packFile);
            String packName = packFile.getName().replace(".zip", "");
            
            // GitHub Releases のURL をそのまま使用
            ResourcePackInfo info = new ResourcePackInfo(packName, githubUrl, hash);
            packs.add(info);
            
            plugin.getLogger().info("Registered GitHub pack: " + packName + " (SHA1: " + hash + ")");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to register pack: " + packFile.getName());
        }
    }

    /**
     * ローカルフォルダのパックをロード
     */
    public void loadLocalPacks(String baseUrl) {
        if (!packsFolder.exists() || !packsFolder.isDirectory()) {
            return;
        }
        
        File[] files = packsFolder.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files != null) {
            for (File file : files) {
                registerLocalPack(file, baseUrl);
            }
        }
    }

    /**
     * パックを登録（ハッシュ計算）
     */
    private void registerLocalPack(File packFile, String baseUrl) {
        try {
            String hash = calculateSha1(packFile);
            String packName = packFile.getName().replace(".zip", "");
            
            // ローカルサーバーのURLを使用
            String url = baseUrl + packFile.getName();
            
            ResourcePackInfo info = new ResourcePackInfo(packName, url, hash);
            packs.add(info);
            
            plugin.getLogger().info("Registered local pack: " + packName + " (SHA1: " + hash + ")");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to register pack: " + packFile.getName());
        }
    }

    /**
     * SHA1ハッシュを計算
     */
    private String calculateSha1(File file) throws IOException {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            throw new IOException("SHA-1 algorithm not available", e);
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 全パックを取得
     */
    public List<ResourcePackInfo> getAllPacks() {
        return new ArrayList<>(packs);
    }

    /**
     * パック総数
     */
    public int getPackCount() {
        return packs.size();
    }
}
