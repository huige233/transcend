package com.huige233.transcend.entity;

import com.huige233.transcend.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public class RainbowLightning extends LightningBolt {

    private int life = 2;
    private int flashes;
    private long boltSeed;

    public RainbowLightning(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
        this.boltSeed = this.random.nextLong();
        this.flashes = this.random.nextInt(3) + 1;
        this.setVisualOnly(true);
    }

    public RainbowLightning(Level level, double x, double y, double z) {
        super(ModEntities.RAINBOW_LIGHTNING.get(), level);
        this.boltSeed = this.random.nextLong();
        this.flashes = this.random.nextInt(3) + 1;
        this.setPos(x, y, z);
        this.setVisualOnly(true);
    }

    public long getBoltSeed() {
        return boltSeed;
    }

    @Override
    public void tick() {
        baseTick();

        if (this.life == 2) {
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                        10000.0F, 0.8F + this.random.nextFloat() * 0.2F);
            }
        }

        --this.life;
        if (this.life < 0) {
            if (this.flashes == 0) {
                this.discard();
            } else if (this.life < -this.random.nextInt(10)) {
                --this.flashes;
                this.life = 1;
                this.boltSeed = this.random.nextLong();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
