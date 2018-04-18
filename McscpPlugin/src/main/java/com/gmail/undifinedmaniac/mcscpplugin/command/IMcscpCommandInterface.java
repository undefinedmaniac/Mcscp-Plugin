package com.gmail.undifinedmaniac.mcscpplugin.command;

import java.util.Collection;

class PlayerReport {
    public String name, ip, maxHealth, health,
                  hunger, level, world;
}

/**
 * An interface for the MCSCP command interface
 */
public interface IMcscpCommandInterface {
    String sendConsoleCmd(String cmd);
    void sendChatMessage(String username, String message);
    void broadcastMessage(String message);
    int getMaxPlayers();
    int getPlayerCount();
    Collection<String> getPlayerList();
    PlayerReport getPlayerReport(String playerName);
    float getTps();
    double getMaxRam();
    double getTotalRam();
    double getFreeRam();
    double getUsedRam();
    String getMotd();
    void stop();
}
