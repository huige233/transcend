package com.huige233.transcend.util;

import java.math.BigInteger;
import java.util.function.IntUnaryOperator;


/** 将机器大整数数据拆分为可同步的十六位字并重组，同时约束产量不超过余额和缓冲空间。 */
public final class MachineDataValues {
    public static final int BIG_WORDS = 14;
    private static final BigInteger MASK = BigInteger.valueOf(0xffff);

    private MachineDataValues() {}

    public static int word(BigInteger value, int index) {
        if (index < 0 || index >= BIG_WORDS || value.signum() < 0) return 0;
        return value.shiftRight(index * 16).and(MASK).intValue();
    }

    public static BigInteger readBig(IntUnaryOperator data, int offset) {
        BigInteger value = BigInteger.ZERO;
        for (int i = BIG_WORDS - 1; i >= 0; i--) {
            value = value.shiftLeft(16).or(BigInteger.valueOf(data.applyAsInt(offset + i) & 0xffff));
        }
        return value;
    }

    public static int readInt(IntUnaryOperator data, int offset) {
        return (data.applyAsInt(offset) & 0xffff) | ((data.applyAsInt(offset + 1) & 0xffff) << 16);
    }

    public static int production(BigInteger remaining, int rate, int free) {
        return remaining.max(BigInteger.ZERO).min(BigInteger.valueOf(Math.max(0, Math.min(rate, free)))).intValue();
    }
}
