package com.gmail.undifinedmaniac.mcscpplugin.interfaces;

import java.util.logging.Level;

/**
 * An interface for the MCSCP command interface
 */
public interface IMcscpDataFetcher {

    //Server data
    int getMaxPlayers();
    int getPlayerCount();
    String getMotd();

    //Performance data
    float getTps();
    float getMaxRam();
    float getTotalRam();
    float getFreeRam();
    float getUsedRam();

    //Player data
    IMcscpPlayerData getPlayerData(String uuid);

    //Commands
    String sendConsoleCmd(String cmd);
    void sendChatMessage(String username, String message);
    void broadcastMessage(String message);
    void stop();
    void logMessage(Level level, String message);
}
