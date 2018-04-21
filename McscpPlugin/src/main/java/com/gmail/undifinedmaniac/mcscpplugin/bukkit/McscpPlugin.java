package com.gmail.undifinedmaniac.mcscpplugin.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import org.apache.logging.log4j.LogManager;

import java.util.logging.Level;

/**
 * This is the core class for the plugin, it enables and disables
 * all the other parts of the plugin
 */
public final class McscpPlugin extends JavaPlugin {

    private static final org.apache.logging.log4j.core.Logger kLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
    private LogAppender mAppender;

    private McscpPluginConfig mConfig = new McscpPluginConfig(this);;

    private BukkitMcscpTcpServer mServer = null;
    private BukkitTpsCalculator mTpsCalculator = null;

    @Override
    public void onLoad() {
        //Load the config file
        mConfig.loadConfiguration();
    }

    /**
     * Enables the plugin
     */
    @Override
    public void onEnable() {
        mAppender = new LogAppender(this);
        kLogger.addAppender(mAppender);

        mTpsCalculator = new BukkitTpsCalculator();
        mTpsCalculator.runTaskTimer(this, 0, 1);

        //Create the TCP server instance and start it
        mServer = new BukkitMcscpTcpServer(this, mConfig.getAddress(), mConfig.getPort());
        mServer.start();
        mServer.runTaskTimer(this, 0, 1);
    }

    /**
     * Disables the plugin
     */
    @Override
    public void onDisable() {
        kLogger.removeAppender(mAppender);

        if (mServer != null)
            mServer.stop();
        if (mTpsCalculator != null)
            mTpsCalculator.cancel();
    }

    public LogAppender getAppender() {
        return mAppender;
    }

    public void logEvent(String newData) {
        mServer.logEvent(newData);
    }

    /**
     * Prints a message to the server log
     * @param message
     */
    public void logMessage(Level level, String message) {
        getLogger().log(level, message);
    }
}
