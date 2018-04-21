package com.gmail.undifinedmaniac.mcscpplugin.bukkit;

import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpDataFetcher;
import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;

public class BukkitDataFetcher implements IMcscpDataFetcher {

    private static final double BYTES_IN_MB = 1048576L;

    private McscpPlugin mPlugin;
    private Server mServer = Bukkit.getServer();
    private static BukkitCommandSender mSender = new BukkitCommandSender();

    BukkitDataFetcher(McscpPlugin plugin) {
        mPlugin = plugin;
    }

    //Server data
    @Override
    public int getMaxPlayers() {
        return mServer.getMaxPlayers();
    }

    @Override
    public int getPlayerCount() {
        return mServer.getOnlinePlayers().size();
    }

    @Override
    public String getMotd() {
        return mServer.getMotd();
    }

    //Performance data
    @Override
    public float getTps() { return roundToTenths(BukkitTpsCalculator.getTps()); }

    @Override
    public float getMaxRam() {
        return roundToTenths(Runtime.getRuntime().maxMemory() / BYTES_IN_MB);
    }

    @Override
    public float getTotalRam() {
        return roundToTenths(Runtime.getRuntime().totalMemory() / BYTES_IN_MB);
    }

    @Override
    public float getFreeRam() {
        return roundToTenths(Runtime.getRuntime().freeMemory() / BYTES_IN_MB);
    }

    @Override
    public float getUsedRam() {
        return roundToTenths(((Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / BYTES_IN_MB));
    }

    //Player data
    @Override
    public IMcscpPlayerData getPlayerData(String uuid) {
        return new BukkitPlayerData(mServer.getPlayer(UUID.fromString(uuid)));
    }

    //Commands
    @Override
    public String sendConsoleCmd(String cmd) {
        mServer.dispatchCommand(mSender, cmd);
        LinkedList<String> messages = mSender.retrieveMessages();
        StringBuilder response = new StringBuilder();
        for (String message : messages) {
            response.append(message);
        }
        return response.toString();
    }

    @Override
    public void sendChatMessage(String username, String message) {
        String chatMessage = "";
        chatMessage = chatMessage.concat("<" + ChatColor.LIGHT_PURPLE);
        chatMessage = chatMessage.concat("Remote/");
        chatMessage = chatMessage.concat(username);
        chatMessage = chatMessage.concat(ChatColor.WHITE + "> ");
        chatMessage = chatMessage.concat(ChatColor.translateAlternateColorCodes('&', message));
        mServer.broadcastMessage(chatMessage);
    }

    @Override
    public void broadcastMessage(String message) {
        String chatMessage = ChatColor.translateAlternateColorCodes('&', message);
        mServer.broadcastMessage(chatMessage);
    }

    @Override
    public void stop() {
        mServer.shutdown();
    }

    @Override
    public void logMessage(Level level, String message) {
        mPlugin.logMessage(level, message);
    }

    private float roundToTenths(double value) {
        BigDecimal decimal = new BigDecimal(value).setScale(1, RoundingMode.HALF_UP);
        return decimal.floatValue();
    }
}
