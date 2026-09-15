package com.huige233.transcend.tech.research;

import com.huige233.transcend.tech.energy.LongEnergyStorage;
import net.minecraft.nbt.CompoundTag;
import java.math.BigInteger;


/** 维护研究站储能、可消费研究点和换算余数，执行保留精度的兑换、扣费与存档迁移。 */
public final class ResearchEnergyAccount {
    public static final long RF_PER_TECH = 100_000L;
    public static final long TECH_PER_POINT = 100L;
    public static final long RF_PER_POINT = RF_PER_TECH * TECH_PER_POINT;
    private static final BigInteger MAX_POINTS = BigInteger.TEN.pow(64).subtract(BigInteger.ONE);
    private final LongEnergyStorage energy = new LongEnergyStorage(Long.MAX_VALUE);
    private BigInteger points = BigInteger.ZERO;
    private long remainder;

    public long stored() { return energy.stored(); }
    public BigInteger points() { return points; }
    public long remainder() { return remainder; }
    public long receive(long amount, boolean simulate) { return energy.receive(amount, simulate); }

    
    public boolean convertExact(long amount) {
        if (amount <= 0 || energy.stored() < amount) return false;
        BigInteger roomRF = MAX_POINTS.subtract(points).multiply(BigInteger.valueOf(RF_PER_POINT))
                .subtract(BigInteger.valueOf(remainder));
        if (roomRF.compareTo(BigInteger.valueOf(amount)) < 0) return false;
        return convert(amount) == amount;
    }

    public long convert(long limit) {
        BigInteger roomRF = MAX_POINTS.subtract(points).multiply(BigInteger.valueOf(RF_PER_POINT))
                .subtract(BigInteger.valueOf(remainder)).max(BigInteger.ZERO);
        long consumed = energy.extract(roomRF.min(BigInteger.valueOf(Math.max(0L, limit))).longValue());
        
        long fractions = remainder + consumed % RF_PER_POINT;
        points = points.add(BigInteger.valueOf(consumed / RF_PER_POINT))
                .add(BigInteger.valueOf(fractions / RF_PER_POINT));
        remainder = fractions % RF_PER_POINT;
        return consumed;
    }

    public BigInteger spend(BigInteger requested) {
        BigInteger taken = requested == null ? BigInteger.ZERO : requested.max(BigInteger.ZERO).min(points);
        points = points.subtract(taken);
        return taken;
    }

    public void save(CompoundTag tag) {
        tag.putInt("AccountingVersion", 1);
        tag.putLong("Energy", energy.stored());
        tag.putString("Points", points.toString());
        tag.putLong("Remainder", remainder);
    }

    public void load(CompoundTag tag) {
        energy.set(tag.getLong("Energy"));
        String value = tag.getString("Points");
        
        points = tag.getInt("AccountingVersion") == 1 && value.matches("[0-9]{1,64}")
                ? new BigInteger(value).min(MAX_POINTS) : BigInteger.ZERO;
        remainder = Math.max(0L, Math.min(RF_PER_POINT - 1, tag.getLong("Remainder")));
    }
}
