package com.gmail.undifinedmaniac.mcscpplugin.command;

import com.gmail.undifinedmaniac.mcscpplugin.McscpPlugin;
import com.gmail.undifinedmaniac.mcscpplugin.network.McscpClient;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.SocketAddress;

/**
 * Processes McscpCommands and performs their desired actions
 */
public class McscpCommandProcessor {

    private static final Pattern FLAG_PATTERN = Pattern.compile("^SETFLAG:(.+):(.+)$");

    private static McscpCommandProcessor kProcessor = null;
    private static IMcscpCommandInterface kInterface = null;
    private static McscpPlugin kPlugin = null;

    private McscpCommandProcessor() {
    }

    /**
     * Initializes the object
     * @param plugin the plugin object
     */
    public static void initialize(McscpPlugin plugin) {
        kProcessor = new McscpCommandProcessor();
        kInterface = new McscpCommandInterface(plugin);
        kPlugin = plugin;
    }

    /**
     * Returns the instance of the command processor
     * @return the command processor
     */
    public static McscpCommandProcessor getInstance() {
        return kProcessor;
    }

    /**
     * Executes a command
     * @param command the command to execute
     */
    public void processCommand(McscpCommand command) {
        switch (command.getType()) {
            case Console: {
                String consoleCmd = command.getData().get(0).substring(4);
                command.setReply(kInterface.sendConsoleCmd(consoleCmd));
                break;
            }
            case Chat: {
                String username = command.getData().get(0).substring(5);
                String message = command.getData().get(1).substring(8);
                kInterface.sendChatMessage(username, message);
                break;
            }
            case Broadcast: {
                kInterface.broadcastMessage(command.getData().get(0).substring(10));
                break;
            }
            case GetMaxPlayers: {
                String reply = String.valueOf(kInterface.getMaxPlayers());
                command.setReply(reply);
                break;
            }
            case GetPlayerCount: {
                String reply = String.valueOf(kInterface.getPlayerCount());
                command.setReply(reply);
                break;
            }
            case GetPlayerList: {
                Collection<String> playerNames = kInterface.getPlayerList();
                StringBuilder builder = new StringBuilder();
                builder.append("BEGINPLAYERLIST\r\n");
                for (String name : playerNames) {
                    builder.append(name);
                    builder.append("\r\n");
                }
                builder.append("ENDPLAYERLIST");
                command.setReply(builder.toString());
                break;
            }
            case GetPlayerReport: {
                String playerName = command.getData().get(0).substring(16);
                command.setReply(kInterface.getPlayerReport(playerName));
                break;
            }
            case GetTps: {
                String reply = String.valueOf(kInterface.getTps());
                command.setReply(reply);
                break;
            }
            case GetPerformanceReport: {
                StringBuilder builder = new StringBuilder();
                builder.append("BEGINPERFORMANCEREPORT");
                builder.append("\r\nTPS:");
                builder.append(kInterface.getTps());
                builder.append("\r\nMAXRAM:");
                builder.append(kInterface.getMaxRam());
                builder.append("\r\nTOTALRAM:");
                builder.append(kInterface.getTotalRam());
                builder.append("\r\nFREERAM:");
                builder.append(kInterface.getFreeRam());
                builder.append("\r\nUSEDRAM:");
                builder.append(kInterface.getUsedRam());
                builder.append("\r\nENDPERFORMANCEREPORT");
                command.setReply(builder.toString());
                break;
            }
            case GetMaxRam: {
                String reply = String.valueOf(kInterface.getMaxRam());
                command.setReply(reply);
                break;
            }
            case GetTotalRam: {
                String reply = String.valueOf(kInterface.getTotalRam());
                command.setReply(reply);
                break;
            }
            case GetFreeRam: {
                String reply = String.valueOf(kInterface.getFreeRam());
                command.setReply(reply);
                break;

            }
            case GetUsedRam: {
                String reply = String.valueOf(kInterface.getUsedRam());
                command.setReply(reply);
                break;
            }
            case GetMotd: {
                command.setReply(kInterface.getMotd());
                break;
            }
            case Stop: {
                kInterface.stop();
                break;
            }
            case SetFlag: {
                boolean success = false;

                Matcher flagMatcher = FLAG_PATTERN.matcher(command.getData().get(0));
                if (flagMatcher.find()) {
                    String flagNameString = flagMatcher.group(1).toUpperCase();
                    String flagValueString = flagMatcher.group(2).toUpperCase();

                    McscpClient.Flag flagName;
                    switch (flagNameString) {
                        case "REPORTPLAYERJOIN": {
                            flagName = McscpClient.Flag.ReportPlayerJoin;
                            break;
                        }
                        case "REPORTPLAYERLEAVE": {
                            flagName = McscpClient.Flag.ReportPlayerLeave;
                            break;
                        }
                        case "CMDRESPONSE": {
                            flagName = McscpClient.Flag.CmdResponse;
                            break;
                        }
                        case "REPORTCHATUPDATE": {
                            flagName = McscpClient.Flag.ReportChatUpdate;
                            break;
                        }
                        case "REPORTPLAYERDEATH": {
                            flagName = McscpClient.Flag.ReportPlayerDeath;
                            break;
                        }
                        default: {
                            flagName = null;
                            break;
                        }
                    }

                    if (flagName != null) {
                        Boolean flagValue;
                        switch (flagValueString) {
                            case "TRUE": {
                                flagValue = true;
                                break;
                            }
                            case "FALSE": {
                                flagValue = false;
                                break;
                            }
                            default: {
                                flagValue = null;
                            }
                        }

                        if (flagValue != null) {
                            command.getClient().setFlag(flagName, flagValue);
                            success = true;
                        }
                    }
                }

                if (!success)
                    invalidCommandError(command.getClient());
                break;
            }
            case Ping: {
                command.setReply("PONG");
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
        kPlugin.printMsg("ERROR: client sent invalid command: " + remoteAddress);
    }
}
