package com.gmail.undifinedmaniac.mcscpplugin.bukkit;

import com.gmail.undifinedmaniac.mcscpplugin.interfaces.IMcscpPlayerData;
import org.bukkit.entity.Player;

public class BukkitPlayerData implements IMcscpPlayerData {

    private Player mPlayer;

    BukkitPlayerData(Player player) {
        mPlayer = player;
    }

    @Override
    public String getUniqueId() {
        return mPlayer.getUniqueId().toString();
    }

    @Override
    public String getName() {
        return mPlayer.getName();
    }

    @Override
    public String getDisplayName() {
        return mPlayer.getDisplayName();
    }

    @Override
    public String getIpAddress() {
        return mPlayer.getAddress().toString();
    }

    @Override
    public float getMaxHealth() {
        return (float)mPlayer.getMaxHealth();
    }

    @Override
    public float getHealth() {
        return (float)mPlayer.getHealth();
    }

    @Override
    public float getHunger() {
        return mPlayer.getFoodLevel();
    }

    @Override
    public float getLevel() {
        return mPlayer.getLevel();
    }

    @Override
    public String getWorld() {
        return mPlayer.getWorld().getName();
    }
}
