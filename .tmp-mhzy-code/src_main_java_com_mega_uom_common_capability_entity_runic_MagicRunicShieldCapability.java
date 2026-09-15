package com.mega.uom.common.capability.entity.runic;

import com.mega.uom.common.capability.ModCapabilities;
import com.mega.uom.common.network.PacketHandler;
import com.mega.uom.common.network.s2c.entity.EntityCapabilitySyncPacket;
import com.mega.uom.common.network.s2c.render.RandomTextDancingRendererPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

public class MagicRunicShieldCapability implements IRunicShieldCapability {
    String name = "capability.fantasy_ending.runic_shield.name";
    short level = 0;
    long tickCount;
    long lifeTime = 0;
    float damageResistance = 0;
    float outResistance = 0;
    int perCooldowns = 0;
    int cooldowns = 0;
    boolean doSpawnRunic = false;
    int runicColor = 0;
    int runicPeriod = 0;
    int hurtTime = 0;
    boolean isDirty = false;

    @Override
    public String getTranslationKeyName() {
        return this.name;
    }

    @Override
    public void setTranslationKeyName(String name) {
        this.name = name;
        this.setDirty(true);
    }

    @Override
    public short getLevel() {
        return this.level;
    }

    @Override
    public void setLevel(short level) {
        this.level = level;
        this.setDirty(true);
    }

    @Override
    public float getDamageResistance() {
        return this.damageResistance;
    }

    @Override
    public void setDamageResistance(float value) {
        this.damageResistance = value;
        this.setDirty(true);
    }

    @Override
    public int hurtTime() {
        return hurtTime;
    }

    @Override
    public void setHurtTime(int time) {
        this.hurtTime = time;
        setDirty(true);
    }

    @Override
    public int getPerCooldowns() {
        return this.perCooldowns;
    }

    @Override
    public void setPerCooldowns(int perCooldowns) {
        this.perCooldowns = perCooldowns;
        this.setDirty(true);
    }

    @Override
    public int getCooldowns() {
        return this.cooldowns;
    }

    @Override
    public void setCooldowns(int cooldowns) {
        this.cooldowns = cooldowns;
        this.setDirty(true);
    }

    @Override
    public float getOutResistance() {
        return this.outResistance;
    }

    @Override
    public void setOutResistance(float value) {
        this.outResistance = value;
        this.setDirty(true);
    }

    @Override
    public int getRunicColor() {
        return this.runicColor;
    }

    @Override
    public void setRunicColor(int runicColor) {
        this.runicColor = runicColor;
        this.setDirty(true);
    }

    @Override
    public int runicPeriod() {
        return this.runicPeriod;
    }

    @Override
    public void setRunicPeriod(int runicPeriod) {
        this.runicPeriod = runicPeriod;
        this.setDirty(true);
    }

    @Override
    public boolean doSpawnRunic() {
        return this.doSpawnRunic;
    }

    @Override
    public void setSpawnRunic(boolean spawnRunic) {
        this.doSpawnRunic = spawnRunic;
        this.setDirty(true);
    }

    @Override
    public long tickCount() {
        return this.tickCount;
    }

    @Override
    public long maxLifeTime() {
        return lifeTime;
    }

    @Override
    public void setMaxLifeTime(long time) {
        this.lifeTime = time;
        this.setDirty(true);
    }

    /**
     * 双端执行{@link com.mega.uom.event.eventhandler.common.CapabilityHandler.Entity#onLivingTick(LivingEvent.LivingTickEvent)}
     *
     * @param living 实体实例
     */

    @Override
    public void tick(LivingEntity living) {
        if (this.level == 0) {
            if (tickCount > 0) {
                tickCount = 0;
                setMaxLifeTime(0);
                setPerCooldowns(0);
                setDamageResistance(0);
                setOutResistance(0);
                setCooldowns(0);
                setHurtTime(0);
                if (!living.level().isClientSide)
                    PacketHandler.sendToAll(new EntityCapabilitySyncPacket(living.getId(), ModCapabilities.ID_CAP_MAGIC_SHIELD_EC.id, this.serializeNBT()));
            }
            return;
        }
        if (this.hurtTime > 0) this.setHurtTime(this.hurtTime - 1);
        if (this.tickCount >= maxLifeTime()) {
            this.setLevel((short) 0);
            return;
        }
        if (this.doSpawnRunic())
            if (!living.level().isClientSide && this.tickCount % this.runicPeriod == 0) {
                Vec3 pos = living.position().add(living.getViewVector(1.0F)).add(Math.random() * 3 - 1.5d, Math.random() * 3 - 1.5d, Math.random() * 3 - 1.5d);
                PacketHandler.sendToAll(new RandomTextDancingRendererPacket(pos.x, pos.y, pos.z, living.getRandom().nextDouble() * 0.08D - 0.04D, living.getRandom().nextDouble() * 0.08D - 0.04D, living.getRandom().nextDouble() * 0.08D - 0.04D, 140, this.runicColor));

            }
        if (!living.level().isClientSide && this.isDirty()) {
            this.setDirty(false);
            PacketHandler.sendToAll(new EntityCapabilitySyncPacket(living.getId(), ModCapabilities.ID_CAP_MAGIC_SHIELD_EC.id, this.serializeNBT()));
        }
        tickCount++;
        if (this.cooldowns > 0)
            this.setCooldowns(this.getCooldowns() - 1);
        IRunicShieldCapability.super.tick(living);
    }

    @Override
    public void onHurt(LivingEntity direct, DamageSource damageSource, float amount, LivingAttackEvent context) {
        if (this.level <= 0) return;
        IRunicShieldCapability.super.onHurt(direct, damageSource, amount, context);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("name", this.name);
        compoundTag.putShort("level", this.level);
        compoundTag.putLong("tickCount", this.tickCount);
        compoundTag.putFloat("damageResistance", this.damageResistance);
        compoundTag.putFloat("outResistance", this.outResistance);
        compoundTag.putInt("perCooldowns", this.perCooldowns);
        compoundTag.putInt("cooldowns", this.cooldowns);
        compoundTag.putInt("runicColor", this.runicColor);
        compoundTag.putInt("runicPeriod", this.runicPeriod);
        compoundTag.putBoolean("doSpawnRunic", this.doSpawnRunic);
        compoundTag.putLong("maxLifeTime", this.lifeTime);
        compoundTag.putInt("hurtTime", this.hurtTime);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt != null) {
            this.setTranslationKeyName(nbt.getString("name"));
            this.setLevel(nbt.getShort("level"));
            this.tickCount = nbt.getLong("tickCount");
            this.setDamageResistance(nbt.getFloat("damageResistance"));
            this.setOutResistance(nbt.getFloat("outResistance"));
            this.setPerCooldowns(nbt.getInt("perCooldowns"));
            this.setCooldowns(nbt.getInt("cooldowns"));
            this.setRunicColor(nbt.getInt("runicColor"));
            this.setRunicPeriod(nbt.getInt("runicPeriod"));
            this.setSpawnRunic(nbt.getBoolean("doSpawnRunic"));
            this.setMaxLifeTime(nbt.getLong("maxLifeTime"));
            this.setHurtTime(nbt.getInt("hurtTime"));
        }
    }

    @Override
    public boolean isDirty() {
        return this.isDirty;
    }

    @Override
    public void setDirty(boolean value) {
        this.isDirty = value;
    }
}
