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
                String consoleCmd = command.getMatcher().group(1);
                command.setReply(kInterface.sendConsoleCmd(consoleCmd));
                break;
            }
            case Chat: {
                String username = command.getMatcher().group(1);
                String message = command.getMatcher().group(2);
                kInterface.sendChatMessage(username, message);
                break;
            }
            case Broadcast: {
                kInterface.broadcastMessage(command.getMatcher().group(1));
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
                builder.append("[PLAYERLIST]");
                for (String name : playerNames)
                    builder.append(String.format(":[%s]", name));

                command.setReply(builder.toString());
                break;
            }
            case GetPlayerReport: {
                String playerName = command.getMatcher().group(1);

                PlayerReport report = kInterface.getPlayerReport(playerName);

                StringBuilder builder = new StringBuilder();
                builder.append("[PLAYERREPORT]:");
                builder.append(String.format("[NAME:%s]:", report.name));
                builder.append(String.format("[IP:%s]:", report.ip));
                builder.append(String.format("[MAXHEALTH:%s]:", report.maxHealth));
                builder.append(String.format("[HEALTH:%s]:", report.health));
                builder.append(String.format("[HUNGER:%s]:", report.hunger));
                builder.append(String.format("[LEVEL:%s]:", report.level));
                builder.append(String.format("[WORLD:%s]", report.world));

                command.setReply(builder.toString());
                break;
            }
            case GetTps: {
                String reply = String.valueOf(kInterface.getTps());
                command.setReply(reply);
                break;
            }
            case GetPerformanceReport: {
                StringBuilder builder = new StringBuilder();
                builder.append("[PERFORMANCEREPORT]:");
                builder.append(String.format("[TPS:%s]:", kInterface.getTps()));
                builder.append(String.format("[MAXRAM:%s]:", kInterface.getMaxRam()));
                builder.append(String.format("[TOTALRAM:%s]:", kInterface.getTotalRam()));
                builder.append(String.format("[FREERAM:%s]:", kInterface.getFreeRam()));
                builder.append(String.format("[USEDRAM:%s]", kInterface.getUsedRam()));

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
            case GetLog: {
                command.setReply(String.format("[LOG]:[DATA:%s]",
                        kPlugin.getAppender().getEntireLog()));
                break;
            }
            case Stop: {
                kInterface.stop();
                break;
            }
            case SetFlag: {
                boolean success = false;

                Matcher flagMatcher = command.getMatcher();
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
                    case "SENDSERVERLOG": {
                        flagName = McscpClient.Flag.SendServerLog;
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
                            break;
                        }
                    }

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
        kPlugin.printMsg("ERROR: client sent invalid command: " + remoteAddress);
    }
}
