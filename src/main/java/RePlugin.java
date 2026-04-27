package net.clusters_prj.replugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class RePlugin extends JavaPlugin implements Listener {

    private ResourcePackManager packManager;
    private ResourcePackServer packServer;
    private BukkitTask packDistributionTask;
    private final Queue<UUID> pendingPlayers = new ConcurrentLinkedQueue<>();
    private static final long PACK_SEND_DELAY = 20L; // 1 tick = 50ms, 20L = 1秒

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // config.yml と packs.yml をリロード
        reloadConfig();
        
        // ResourcePackManager を初期化
        packManager = new ResourcePackManager(this);
        
        // ローカルサーバーのポートを取得
        int serverPort = getConfig().getInt("server.port", 8765);
        String serverHost = getConfig().getString("server.host", "localhost");
        String servePath = getConfig().getString("server.serve-path", "/");
        
        // ResourcePackServer を起動
        File packsFolder = new File(getDataFolder(), "packs");
        packServer = new ResourcePackServer(this, serverPort, packsFolder, servePath);
        packServer.start();
        
        // GitHub Releasesからダウンロード
        if (getConfig().getBoolean("github.enabled", true)) {
            packManager.downloadFromGithub(getConfig().getConfigurationSection("github"));
        }
        
        // ローカルフォルダからロード（クライアント側のURL指定）
        String clientBaseUrl = getConfig().getString("client.base-url", "http://localhost:8765/");
        packManager.loadLocalPacks(clientBaseUrl);
        
        // イベント登録
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // リソースパック配布用タスク開始
        startPackDistributionTask();
        
        getLogger().info("RePlugin enabled! Loaded " + packManager.getPackCount() + " resource packs.");
    }

    @Override
    public void onDisable() {
        if (packDistributionTask != null) {
            packDistributionTask.cancel();
        }
        if (packServer != null) {
            packServer.stop();
        }
        getLogger().info("RePlugin disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // キューに追加（配布タスクで処理）
        pendingPlayers.offer(player.getUniqueId());
    }

    /**
     * リソースパック配布用タスク（1秒ごとに1プレイヤーに送信）
     */
    private void startPackDistributionTask() {
        packDistributionTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!pendingPlayers.isEmpty()) {
                UUID playerUuid = pendingPlayers.poll();
                Player player = Bukkit.getPlayer(playerUuid);
                
                if (player != null && player.isOnline()) {
                    List<ResourcePackInfo> packs = packManager.getAllPacks();
                    for (ResourcePackInfo pack : packs) {
                        player.setResourcePack(pack.getUrl(), pack.getHash());
                        getLogger().info("Sent pack '" + pack.getName() + "' to " + player.getName());
                    }
                }
            }
        }, 20L, 20L); // 1秒ごと
    }

    public ResourcePackManager getPackManager() {
        return packManager;
    }
}
