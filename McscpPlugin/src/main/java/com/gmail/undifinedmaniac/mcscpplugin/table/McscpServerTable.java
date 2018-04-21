package com.gmail.undifinedmaniac.mcscpplugin.table;

import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpDataFetcher;
import com.gmail.undifinedmaniac.mcscpplugin.network.McscpTcpServer;

public class McscpServerTable extends McscpTableBase<McscpServerTable.Key> {
    private IMcscpDataFetcher mFetcher;
    private McscpTcpServer mServer;

    public enum Key {
        MaxPlayers, PlayerCount, Motd, Tps, MaxRam,
        TotalRam, UsedRam
    }

    public McscpServerTable(IMcscpDataFetcher fetcher, McscpTcpServer server) {
        super();
        mFetcher = fetcher;
        mServer = server;
    }

    public void updateKeys() {
        String maxPlayers = String.valueOf(mFetcher.getMaxPlayers());
        String playerCount = String.valueOf(mFetcher.getPlayerCount());
        String motd = String.valueOf(mFetcher.getMotd());
        String tps = String.valueOf(mFetcher.getTps());
        String maxRam = String.valueOf(mFetcher.getMaxRam());
        String totalRam = String.valueOf(mFetcher.getTotalRam());
        String usedRam = String.valueOf(mFetcher.getUsedRam());

        updateKeysHelper(Key.MaxPlayers, maxPlayers);
        updateKeysHelper(Key.PlayerCount, playerCount);
        updateKeysHelper(Key.Motd, motd);
        updateKeysHelper(Key.Tps, tps);
        updateKeysHelper(Key.MaxRam, maxRam);
        updateKeysHelper(Key.TotalRam, totalRam);
        updateKeysHelper(Key.UsedRam, usedRam);
    }

    private void updateKeysHelper(Key key, String value) {
        if (updateKey(key, value))
            mServer.serverTableUpdate(key, value);
    }
}
