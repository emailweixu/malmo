package com.microsoft.Malmo.Utils;

import net.minecraft.util.Timer;

public class LockSteppedTimer extends Timer {
    LockSteppedTimer() {
        super(1);
        this.elapsedPartialTicks = 0;
        this.renderPartialTicks = 0;
    }

    public void updateTimer() {
        this.elapsedTicks = 1;
        this.elapsedPartialTicks = 0;
        this.renderPartialTicks = 0;
    }
}
