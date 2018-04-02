package com.gmail.undifinedmaniac.mcscpplugin;

/**
 * This class simply creates/reads a config file that contains
 * The address and port for the server
 */
public class McscpPluginConfig {

    private McscpPlugin mPlugin;

    public McscpPluginConfig(McscpPlugin plugin) {
        mPlugin = plugin;
    }

    /**
     * Loads the config for the plugin
     */
    public void loadConfiguration() {
        mPlugin.getConfig().addDefault("address", "127.0.0.1");
        mPlugin.getConfig().addDefault("port", 54620);
        mPlugin.getConfig().options().copyDefaults(true);
        mPlugin.saveConfig();
    }

    /**
     * Gets the address for the server from the config
     * @return the address
     */
    public String getAddress() {
        return mPlugin.getConfig().getString("address");
    }

    /**
     * Gets the port for the server from the config
     * @return the port
     */
    public int getPort() {
        return mPlugin.getConfig().getInt("port");
    }
}
