package com.mega.uom.common.capability.entity.runic;

import com.mega.uom.common.capability.SyncNBTSerializable;
import com.mega.uom.common.network.PacketHandler;
import com.mega.uom.client.render.custom.normal.RuneRenderer;
import com.mega.uom.util.entity.EntityActuallyHurt;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public interface IRunicShieldCapability extends SyncNBTSerializable {
    /**
     * @return 此护盾名称
     */
    String getTranslationKeyName();

    void setTranslationKeyName(String name);

    short getLevel();

    void setLevel(short level);

    /**
     * 每次伤害减免值
     *
     * @return float类型数值
     */
    float getDamageResistance();

    void setDamageResistance(float value);

    /**
     * 监听到{@link LivingAttackEvent}事件时，产生的时间变量[0, hurtDuration]
     *
     * @return 时间(in ticks)
     */
    int hurtTime();

    void setHurtTime(int time);

    /**
     * 当伤害超过{@link IRunicShieldCapability#getDamageResistance()}时造成的冷却时间
     * 为0时应当不进行冷却
     * 冷却时间内收到的伤害超过{@link IRunicShieldCapability#getDamageResistance()}三分之一时再次冷却
     *
     * @return cooldowns (in ticks)
     */
    int getPerCooldowns();

    void setPerCooldowns(int perCooldowns);

    /**
     * 当前冷却时间
     *
     * @return 时间(in ticks)
     */
    int getCooldowns();

    void setCooldowns(int cooldowns);

    /**
     * 当伤害超过{@link IRunicShieldCapability#getDamageResistance()}时,对额外伤害造成的减伤(区间[0, 1])
     *
     * @return 减免百分比
     */
    float getOutResistance();

    void setOutResistance(float value);

    /**
     * 四散开来的如尼字符数量
     * 渲染见:{@link RuneRenderer}
     *
     * @return
     */
    int getRunicColor();

    void setRunicColor(int runicColor);

    /**
     * 生成如尼字符的间隔
     *
     * @return 间隔(in ticks)
     */
    int runicPeriod();

    void setRunicPeriod(int runicPeriod);

    /**
     * @return 是否生成如尼字符
     */
    boolean doSpawnRunic();

    void setSpawnRunic(boolean spawnRunic);

    /**
     * {@link IRunicShieldCapability#tick(LivingEntity)}更新以来的总时间
     *
     * @return 总时间(in ticks)
     */
    long tickCount();

    /**
     * 最大存在时间
     *
     * @return 时间(in ticks)
     */
    long maxLifeTime();

    void setMaxLifeTime(long time);

    /**
     * @param direct       受击实体
     * @param damageSource 伤害源
     * @param amount       伤害值
     */
    default void onHurt(LivingEntity direct, DamageSource damageSource, float amount, LivingAttackEvent context) {
        if (direct == null || getLevel() == 0 || getDamageResistance() <= 0F || getOutResistance() <= 0F) return;
        if (this instanceof MagicRunicShieldCapability magicCap) {
            magicCap.tickCount+=(int) (amount / 5F);
            if (EntityActuallyHurt.isApollyon(damageSource.getEntity()))
                magicCap.tickCount+=direct.getRandom().nextInt(10, 25);
        }
        if (this.getCooldowns() > 0 && amount > this.getDamageResistance() * 0.33F) {
            this.setCooldowns(this.getPerCooldowns());
            return;
        }
        if (amount > 1e-6F) {
            boolean bypass = false;
            if (damageSource.is(DamageTypeTags.IS_FIRE) && direct.hasEffect(MobEffects.FIRE_RESISTANCE)) bypass = true;
            else if (direct.isInvulnerableTo(damageSource)) bypass = true;
            if (!bypass) {
                this.setHurtTime(10);
                if ((damageSource.getEntity() != null || !damageSource.is(DamageTypes.GENERIC_KILL)) && direct.level() instanceof ServerLevel serverLevel && (hurtTime() <= 0 || direct.tickCount % 5 == 0)) {
                    PacketHandler.playSound(serverLevel, direct, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.25F, (float) direct.getRandom().triangle(1.5F, 0.3F));
                }
            }
        }
        if (amount <= getDamageResistance()) {
            if ((damageSource.getEntity() != null || !damageSource.is(DamageTypes.GENERIC_KILL)))
                if (!damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || amount <= getDamageResistance()*0.5F)
                    context.setCanceled(true);
        } else {
            this.setCooldowns(this.getPerCooldowns());
        }
    }

    default void dealDamage(LivingEntity direct, DamageSource damageSource, float amount, LivingHurtEvent context) {
        if (direct == null || getLevel() == 0 || getDamageResistance() <= 0F || getOutResistance() <= 0F) return;
        context.setAmount(context.getAmount() - this.getDamageResistance());
        if (context.getAmount() > 0 && getOutResistance() >= 0F) context.setAmount(amount * (1F - getOutResistance()));

    }

    default void tick(LivingEntity living) {
    }

    @OnlyIn(Dist.CLIENT)
    default float partialTicks() {
        return tickCount() + Minecraft.getInstance().getPartialTick();
    }
}
