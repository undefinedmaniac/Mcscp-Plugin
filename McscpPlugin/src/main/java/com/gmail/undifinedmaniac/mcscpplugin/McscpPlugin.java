package com.gmail.undifinedmaniac.mcscpplugin;

import com.gmail.undifinedmaniac.mcscpplugin.network.*;
import com.gmail.undifinedmaniac.mcscpplugin.command.McscpCommandProcessor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This is the core class for the plugin, it enables and disables
 * all the other parts of the plugin
 */
public final class McscpPlugin extends JavaPlugin {

    private McscpTcpServer mServer = null;

    private static float tps;

    /**
     * Enables the plugin
     */
    @Override
    public void onEnable() {
        //Init the command processor so that it is ready to handle commands
        McscpCommandProcessor.initialize(this);

        //Load the config file
        McscpPluginConfig config = new McscpPluginConfig(this);
        config.loadConfiguration();

        //Create the TCP server instance and start it
        mServer = new McscpTcpServer(this, config.getAddress(), config.getPort());
        mServer.start();
        mServer.runTaskTimer(this, 0, 1);

        //Start a repeating task to calculate the servers TPS
        Bukkit.getServer().getScheduler().runTaskTimer(this, new Runnable() {

            long secstart;
            long secend;

            int ticks;

            @Override
            public void run() {
                secstart = (System.currentTimeMillis() / 1000);

                if (secstart == secend) {
                    ticks++;
                } else {
                    secend = secstart;
                    tps = (tps == 0) ? ticks : ((tps + ticks) / 2);
                    ticks = 1;
                }
            }

        }, 0, 1);
    }

    /**
     * Disables the plugin
     */
    @Override
    public void onDisable() {
        if (mServer != null)
            mServer.stop();
    }

    /**
     * Gets the TPS of the server
     * @return
     */
    public static float getTPS() {
        return tps;
    }

    /**
     * Prints a message to the server log
     * @param message
     */
    public void printMsg(String message) {
        getLogger().info(message);
    }
}
