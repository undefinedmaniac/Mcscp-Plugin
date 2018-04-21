package com.gmail.undifinedmaniac.mcscpplugin.command;

import com.gmail.undifinedmaniac.mcscpplugin.network.McscpClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a command from an MCSCP client
 */
public class McscpCommand {

    public enum CommandType {
        Console, Chat, Broadcast, Stop, SetFlag, Ping, Unknown
    }

    private static Pattern CONSOLE_PATTERN = Pattern.compile("(?i)\\[CMD]:\\[CONTENT:(.*)]"),
                           CHAT_PATTERN = Pattern.compile("(?i)\\[MSG]:\\[SENDER:(.*?)]:\\[CONTENT:(.*)]"),
                           BROADCAST_PATTERN = Pattern.compile("(?i)\\[BROADCAST]:\\[CONTENT:(.*)]"),
                           SET_FLAG_PATTERN = Pattern.compile("(?i)\\[SETFLAG]:\\[NAME:(.*)]:\\[VALUE:(.*)]");

    private McscpClient mClient;
    private CommandType mType;
    private String mData, mReply;
    private Matcher mMatcher;

    public McscpCommand(McscpClient client, String data) {
        mClient = client;
        mData = data;
        mReply = null;

        mType = detectCommandType();
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
     * Gets all data for this command
     * @return the data
     */
    public String getData() {
        return mData;
    }

    /**
     * Gets the matcher for this command
     * @return The matcher
     */
    public Matcher getMatcher() {
        return mMatcher;
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
     * Detects the command type
     * @return the type of command
     */
    private CommandType detectCommandType() {

        //Check all of our regexes first
        Matcher matcher = CONSOLE_PATTERN.matcher(mData);
        if (matcher.find()) {
            mMatcher = matcher;
            return CommandType.Console;
        }

        matcher = CHAT_PATTERN.matcher(mData);
        if (matcher.find()) {
            mMatcher = matcher;
            return CommandType.Chat;
        }

        matcher = BROADCAST_PATTERN.matcher(mData);
        if (matcher.find()) {
            mMatcher = matcher;
            return CommandType.Broadcast;
        }

        matcher = SET_FLAG_PATTERN.matcher(mData);
        if (matcher.find()) {
            mMatcher = matcher;
            return CommandType.SetFlag;
        }

        //Then check against static stuff
        String data = mData.toUpperCase();

        if (data.equals("[STOP]"))
            return CommandType.Stop;
        else if (data.equals("[PING]"))
            return CommandType.Ping;

        return CommandType.Unknown;
    }
}
