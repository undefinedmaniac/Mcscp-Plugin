package com.gmail.undifinedmaniac.mcscpplugin.command;

import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpDataFetcher;
import com.gmail.undifinedmaniac.mcscpplugin.network.McscpClient;

import java.util.logging.Level;
import java.util.regex.Matcher;

import java.net.SocketAddress;

/**
 * Processes McscpCommands and performs their desired actions
 */
public class McscpCommandProcessor {

    private IMcscpDataFetcher mFetcher = null;

    public McscpCommandProcessor(IMcscpDataFetcher fetcher) {
        mFetcher = fetcher;
    }

    /**
     * Executes a command
     * @param command the command to execute
     */
    public void processCommand(McscpCommand command) {
        switch (command.getType()) {
            case Console: {
                String consoleCmd = command.getMatcher().group(1);
                command.setReply(mFetcher.sendConsoleCmd(consoleCmd));
                break;
            }
            case Chat: {
                String username = command.getMatcher().group(1);
                String message = command.getMatcher().group(2);
                mFetcher.sendChatMessage(username, message);
                break;
            }
            case Broadcast: {
                mFetcher.broadcastMessage(command.getMatcher().group(1));
                break;
            }
            case Stop: {
                mFetcher.stop();
                break;
            }
            case SetFlag: {
                boolean success = false;

                Matcher flagMatcher = command.getMatcher();
                String flagNameString = flagMatcher.group(1).toUpperCase();
                String flagValueString = flagMatcher.group(2).toUpperCase();

                McscpClient.Flag flagName = McscpClient.getFlagType(flagNameString);

                if (flagName != null) {
                    Boolean flagValue = McscpClient.convertStringToBool(flagValueString);

                    if (flagValue != null) {
                        command.getClient().setFlag(flagName, flagValue);
                        success = true;
                    }
                }

                if (!success)
                    invalidCommandError(command.getClient());
                break;
            }
            case Ping: {
                command.setReply("[PONG]");
                break;
            }
            default: {
                invalidCommandError(command.getClient());
                break;
            }
        }
    }

    /**
     * Prints an error message when an invalid command is processed
     * @param client the offender
     */
    private void invalidCommandError(McscpClient client) {
        SocketAddress remoteAddress = client.address();
        mFetcher.logMessage(Level.WARNING, "ERROR: client sent invalid command: " + remoteAddress);
    }
}
