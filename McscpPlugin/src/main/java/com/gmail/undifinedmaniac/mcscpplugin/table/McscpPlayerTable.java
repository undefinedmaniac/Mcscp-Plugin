package com.gmail.undifinedmaniac.mcscpplugin.table;

import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpDataFetcher;
import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpPlayerData;
import com.gmail.undifinedmaniac.mcscpplugin.network.McscpTcpServer;

public class McscpPlayerTable extends McscpTableBase<McscpPlayerTable.Key> {
    private String mUuid;
    private McscpTcpServer mServer;
    private IMcscpPlayerData mPlayer;

    public enum Key {
        Name, DisplayName, Ip, World, MaxHealth,
        Health, Hunger, Level
    }

    public McscpPlayerTable(String uuid, IMcscpDataFetcher fetcher, McscpTcpServer server) {
        mUuid = uuid;
        mServer = server;
        mPlayer = fetcher.getPlayerData(uuid);
    }

    public void updateKeys() {
        String name = mPlayer.getName();
        String displayName = mPlayer.getDisplayName();
        String ip = mPlayer.getIpAddress();
        String world = mPlayer.getWorld();
        String maxHealth = String.valueOf(mPlayer.getMaxHealth());
        String health = String.valueOf(mPlayer.getHealth());
        String hunger = String.valueOf(mPlayer.getHunger());
        String level = String.valueOf(mPlayer.getLevel());

        updateKeysHelper(Key.Name, name);
        updateKeysHelper(Key.DisplayName, displayName);
        updateKeysHelper(Key.Ip, ip);
        updateKeysHelper(Key.World, world);
        updateKeysHelper(Key.MaxHealth, maxHealth);
        updateKeysHelper(Key.Health, health);
        updateKeysHelper(Key.Hunger, hunger);
        updateKeysHelper(Key.Level, level);
    }

    private void updateKeysHelper(Key key, String value) {
        if (updateKey(key, value))
            mServer.playerTableUpdate(mUuid, key, value);
    }
}
