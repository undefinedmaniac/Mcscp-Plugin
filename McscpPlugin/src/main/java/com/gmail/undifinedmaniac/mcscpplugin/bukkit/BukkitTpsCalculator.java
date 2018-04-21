package com.gmail.undifinedmaniac.mcscpplugin.bukkit;

import org.bukkit.scheduler.BukkitRunnable;

public class BukkitTpsCalculator extends BukkitRunnable {
    private long mCurrentSecond;
    private int mTickCount;

    private static float kTps = 0;

    public static float getTps() {
        return kTps;
    }

    @Override
    public void run() {
        long mStartingSecond = (System.currentTimeMillis() / 1000);

        if (mStartingSecond == mCurrentSecond) {
            mTickCount++;
        } else {
            mCurrentSecond = mStartingSecond;
            kTps = (kTps == 0) ? mTickCount : ((kTps + mTickCount) / 2);
            mTickCount = 1;
        }
    }
}
