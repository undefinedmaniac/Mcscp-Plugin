package com.gmail.undifinedmaniac.mcscpplugin.command;

import java.util.Collection;

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
    String getPlayerReport(String playerName);
    float getTps();
    double getMaxRam();
    double getTotalRam();
    double getFreeRam();
    double getUsedRam();
    String getMotd();
    void stop();
}
