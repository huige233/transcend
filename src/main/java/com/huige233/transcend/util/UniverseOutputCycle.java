package com.huige233.transcend.util;

import net.minecraftforge.energy.IEnergyStorage;
import java.math.BigInteger;


/** 以普通或超频节奏将宇宙剩余能量分批填入缓冲，并在每次脉冲后推动能量输出。 */
public final class UniverseOutputCycle {
    private int remainder;

    public void reset() { remainder = 0; }

    public BigInteger tick(boolean overclock, BigInteger remaining, IEnergyStorage buffer, Runnable push) {
        remainder += overclock ? 128 : 20;
        int pulses = remainder / 20;
        remainder %= 20;
        for (int i = 0; i < pulses; i++) {
            int amount = MachineDataValues.production(remaining, Integer.MAX_VALUE,
                    buffer.getMaxEnergyStored() - buffer.getEnergyStored());
            int generated = buffer.receiveEnergy(amount, false);
            if (generated <= 0) {
                push.run();
                break;
            }
            remaining = remaining.subtract(BigInteger.valueOf(generated));
            
            push.run();
        }
        return remaining;
    }
}
