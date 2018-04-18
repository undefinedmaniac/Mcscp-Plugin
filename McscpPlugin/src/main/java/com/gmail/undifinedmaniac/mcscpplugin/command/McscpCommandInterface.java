package com.gmail.undifinedmaniac.mcscpplugin.command;

import com.gmail.undifinedmaniac.mcscpplugin.McscpPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides an interface for processing MCSCP commands
 */
public class McscpCommandInterface implements IMcscpCommandInterface {

    private static final double BYTES_IN_MB = 1048576L;
    private static CustomCommandSender mSender = new CustomCommandSender();

    private McscpPlugin mPlugin;

    public McscpCommandInterface(McscpPlugin plugin) {
        mPlugin = plugin;
    }

    /**
     * Sends a command to the server console
     * @param cmd the command
     * @return the reply from the console
     */
    @Override
    public String sendConsoleCmd(String cmd) {
        Bukkit.getServer().dispatchCommand(mSender, cmd);
        LinkedList<String> messages = mSender.retrieveMessages();
        StringBuilder response = new StringBuilder();
        for (String message : messages) {
            response.append(message);
        }
        return response.toString();
    }

    /**
     * Sends a chat message to the server
     * @param username the username of the player who is sending the message
     * @param message the message to send
     */
    @Override
    public void sendChatMessage(String username, String message) {
        String chatMessage = "";
        chatMessage = chatMessage.concat("<" + ChatColor.LIGHT_PURPLE);
        chatMessage = chatMessage.concat("Remote/");
        chatMessage = chatMessage.concat(username);
        chatMessage = chatMessage.concat(ChatColor.WHITE + "> ");
        chatMessage = chatMessage.concat(ChatColor.translateAlternateColorCodes('&', message));
        Bukkit.getServer().broadcastMessage(chatMessage);
    }

    /**
     * Broadcast a message on the server
     * @param message the message
     */
    @Override
    public void broadcastMessage(String message) {
        String chatMessage = ChatColor.translateAlternateColorCodes('&', message);
        Bukkit.getServer().broadcastMessage(chatMessage);
    }

    /**
     * Gets the maximum allowed players on the server
     * @return the max number of players
     */
    @Override
    public int getMaxPlayers() {
        return Bukkit.getServer().getMaxPlayers();
    }

    /**
     * Gets the current player count of the server
     * @return the current player count
     */
    @Override
    public int getPlayerCount() {
        return Bukkit.getServer().getOnlinePlayers().size();
    }

    /**
     * Gets a list of players on the server
     * @return the player list
     */
    @Override
    public Collection<String> getPlayerList() {
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        List<String> playerNames = new ArrayList<>();
        for (Player player : players)
            playerNames.add(player.getPlayerListName());
        return playerNames;
    }

    /**
     * Get a player report containing lots of info about a player
     * @param playerName the player username
     * @return the player report
     */
    @Override
    public PlayerReport getPlayerReport(String playerName) {
        PlayerReport report = new PlayerReport();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getPlayerListName().equals(playerName)) {
                report.name = player.getDisplayName();
                report.ip = player.getAddress().toString();
                report.maxHealth = Double.toString(player.getMaxHealth());
                report.health = Double.toString(player.getHealth());
                report.hunger = Double.toString(player.getFoodLevel());
                report.level = Double.toString(player.getLevel());
                report.world = player.getWorld().getName();
            }
        }
        return report;
    }

    /**
     * Get the server TPS
     * @return the TPS
     */
    @Override
    public float getTps() {
        return McscpPlugin.getTPS();
    }

    /**
     * Get the max ram of the VM (in MB)
     * @return the max ram
     */
    @Override
    public double getMaxRam() {
        return Runtime.getRuntime().maxMemory() / BYTES_IN_MB;
    }

    /**
     * Get the total ram of the VM (in MB)
     * @return the total ram
     */
    @Override
    public double getTotalRam() {
        return Runtime.getRuntime().totalMemory() / BYTES_IN_MB;
    }

    /**
     * Get free ram of the VM (in MB)
     * @return the free ram
     */
    @Override
    public double getFreeRam() {
        return Runtime.getRuntime().freeMemory() / BYTES_IN_MB;
    }

    /**
     * Get the used ram of the VM (in MB)
     * @return the used ram
     */
    @Override
    public double getUsedRam() {
        return ((Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / BYTES_IN_MB);
    }

    /**
     * Get the MOTD of the server
     * @return the MOTD
     */
    @Override
    public String getMotd() {
        return Bukkit.getMotd();
    }

    /**
     * Stop the server
     */
    @Override
    public void stop() {
        Bukkit.getServer().shutdown();
    }
}
