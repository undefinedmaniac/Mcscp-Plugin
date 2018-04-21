package com.gmail.undifinedmaniac.mcscpplugin.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.gmail.undifinedmaniac.mcscpplugin.network.McscpTcpServer;

public class BukkitMcscpTcpServer extends BukkitRunnable implements Listener {

    private McscpTcpServer mServer;

    BukkitMcscpTcpServer(McscpPlugin plugin, String address, int port) {
        super();
        mServer = new McscpTcpServer(new BukkitDataFetcher(plugin), address, port);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void start() {
        mServer.start();
    }

    public void stop() {
        mServer.stop();
        cancel();
    }

    @Override
    public void run() {
        mServer.processEvents();
    }

    /**
     * Event handler for player join events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        mServer.playerJoinEvent(player.getUniqueId().toString());
    }

    /**
     * Event handler for player leave events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        mServer.playerLeaveEvent(player.getUniqueId().toString());
    }

    /**
     * Event handler for chat update events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatEvent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        mServer.chatEvent(player.getUniqueId().toString(), event.getMessage());
    }

    /**
     * Event handler for player death events
     * @param event the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        mServer.deathEvent(player.getUniqueId().toString(), event.getDeathMessage());
    }

    public void logEvent(String newData) {
        mServer.logEvent(newData);
    }
}
