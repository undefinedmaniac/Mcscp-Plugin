package com.gmail.undifinedmaniac.mcscpplugin.command;

import com.gmail.undifinedmaniac.mcscpplugin.network.McscpClient;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a command from an MCSCP client
 */
public class McscpCommand {

    public enum CommandType {
        Console, Chat, Broadcast, GetMaxPlayers, GetPlayerCount, GetPlayerList,
        GetPlayerReport, GetTps, GetPerformanceReport, GetMaxRam, GetTotalRam,
        GetFreeRam, GetUsedRam, GetMotd, Stop, SetFlag, Ping, Unknown
    }

    private McscpClient mClient;
    private CommandType mType;
    private List<String> mData;
    private String mReply;
    private boolean mReady;

    public McscpCommand(McscpClient client, String data) {
        mClient = client;
        mData = new ArrayList<>();
        mReply = null;

        mData.add(data);
        mType = detectCommandType();
        mReady = checkIfReady();
    }

    /**
     * Gets the associated client for this command
     * @return the client
     */
    public McscpClient getClient() {
        return mClient;
    }

    /**
     * Gets the type of command
     * @return the type of command
     */
    public CommandType getType() {
        return mType;
    }

    /**
     * Add data to the command in the case that
     * data must come in multiple chunks
     * @param data
     */
    public void addData(String data) {
        if (checkNewData(data)) {
            mData.add(data);
            mReady = checkIfReady();
        } else {
            mType = CommandType.Unknown;
            mReady = true;
        }
    }

    /**
     * Gets all data for this command
     * @return the data
     */
    public List<String> getData() {
        return mData;
    }

    /**
     * Sets a reply to send to the client
     * @param reply the reply message
     */
    public void setReply(String reply) {
        mReply = reply;
    }

    /**
     * Gets the reply to send to the client
     * @return the reply message
     */
    public String getReply() {
        return mReply;
    }

    /**
     * Checks if this command has a reply for the client
     * @return true if there is reply, false otherwise
     */
    public boolean hasReply() {
        return mReply != null;
    }

    /**
     * Checks if this command has enough data to be executed
     * @return true if the command is ready, false otherwise
     */
    public boolean isReady() {
        return mReady;
    }

    /**
     * Verify if newly added data is what was expected
     * @param data the new data
     * @return true if it was expected, false otherwise
     */
    private boolean checkNewData(String data) {
        switch (mType) {
            case Chat: {
                return data.startsWith("CONTENT:");
            }
            default: {
                return false;
            }
        }
    }

    /**
     * Detects the command type
     * @return the type of command
     */
    private CommandType detectCommandType() {
        if (mData.size() != 0) {
            String data = mData.get(0).toUpperCase();

            if (data.startsWith("CMD:"))
                return CommandType.Console;
            else if (data.startsWith("CHAT:"))
                return CommandType.Chat;
            else if (data.startsWith("BROADCAST:"))
                return CommandType.Broadcast;
            else if (data.equals("GETMAXPLAYERS"))
                return CommandType.GetMaxPlayers;
            else if (data.equals("GETPLAYERCOUNT"))
                return CommandType.GetPlayerCount;
            else if (data.equals("GETPLAYERLIST"))
                return CommandType.GetPlayerList;
            else if (data.startsWith("GETPLAYERREPORT:"))
                return CommandType.GetPlayerReport;
            else if (data.equals("GETTPS"))
                return CommandType.GetTps;
            else if (data.equals("GETPERFORMANCEREPORT"))
                return CommandType.GetPerformanceReport;
            else if (data.equals("GETMAXRAM"))
                return CommandType.GetMaxRam;
            else if (data.equals("GETTOTALRAM"))
                return CommandType.GetTotalRam;
            else if (data.equals("GETFREERAM"))
                return CommandType.GetFreeRam;
            else if (data.equals("GETUSEDRAM"))
                return CommandType.GetUsedRam;
            else if (data.equals("GETMOTD"))
                return CommandType.GetMotd;
            else if (data.equals("STOP"))
                return CommandType.Stop;
            else if (data.startsWith("SETFLAG:"))
                return CommandType.SetFlag;
            else if (data.equals("PING"))
                return CommandType.Ping;
        }

        return CommandType.Unknown;
    }

    /**
     * Checks if this command has enough data to be executed
     * @return true if the command is ready, false otherwise
     */
    private boolean checkIfReady() {
        switch (mType) {
            case Chat: {
                return (mData.size() >= 2);
            }
            default: {
                return true;
            }
        }
    }
}
