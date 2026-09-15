package com.huige233.transcend;

import com.huige233.transcend.entity.projectile.ParticleBolt;
import com.huige233.transcend.handle.SiriusCombatHandler;
import com.huige233.transcend.tech.combat.BossDefenseRegistry;
import com.huige233.transcend.tech.combat.PenetrationProfile;
import com.huige233.transcend.tech.combat.ShotPenetration;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.registries.ForgeRegistries;


/** 在服务端游戏测试中验证生命适配器、天狼星穿透分摊、禁疗时限和弹体存档行为。 */
@net.minecraftforge.gametest.GameTestHolder(Transcend.MODID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class SiriusCombatGameTests {
    private static boolean cowAdapterRegistered;

    private SiriusCombatGameTests() {}

    private static void registerCowAdapter() {
        if (cowAdapterRegistered) return;
        BossDefenseRegistry.register(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.COW),
                new com.huige233.transcend.tech.combat.BossDefenseAdapter() {
                    public int defenseTier() { return 3; }
                    public boolean allowsPercentDamage(LivingEntity target, DamageSource source) { return true; }
                });
        cowAdapterRegistered = true;
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void healthAdapterWriteIsUsed(GameTestHelper helper) {
        CountingCow target = cow(helper);
        final float[] externalHealth = {3.0F};
        com.huige233.transcend.combat.attack.HealthAccess.register(CountingCow.class,
                new com.huige233.transcend.combat.attack.HealthAccess.Adapter<CountingCow>() {
                    public double get(CountingCow entity) { return externalHealth[0]; }
                    public boolean set(CountingCow entity, double value) {
                        externalHealth[0] = (float) value;
                        return true;
                    }
                });
        check(com.huige233.transcend.combat.attack.HealthAccess.read(target) == 3.0F,
                "adapter read");
        check(com.huige233.transcend.combat.attack.HealthAccess.write(target, 17.0F),
                "adapter write accepted");
        check(externalHealth[0] == 17.0F, "adapter storage updated");
        check(target.getHealth() == 17.0F, "vanilla health remains synchronized");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void penetrationProfileUsesFixedSplit(GameTestHelper helper) {
        registerCowAdapter();
        PenetrationProfile p = ShotPenetration.perProjectile(PenetrationProfile.none(), true, 2);
        check(p.armorStrength() == 4 && p.shieldStrength() == 4 && p.bossStrength() == 4, "tier 4");
        check(p.maxHealthDamagePercent() == 5.0F, "fixed 10 percent split");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void normalBoltDoesNotLockHealing(GameTestHelper helper) {
        registerCowAdapter();
        CountingCow target = cow(helper);
        ParticleBolt bolt = ParticleBolt.shoot(helper.getLevel(), target, Vec3.ZERO, 0, 1, 0, 1, 0, 0, false,
                new PenetrationProfile(0, 0, 3, 10), false);
        check(!bolt.isSirius(), "normal marker");
        LivingDamageEvent damage = new LivingDamageEvent(target,
                TranscendDamage.particleBolt(helper.getLevel(), bolt, target), 1.0F);
        SiriusCombatHandler.onDamage(damage);
        LivingHealEvent heal = new LivingHealEvent(target, 1.0F);
        SiriusCombatHandler.onHeal(heal);
        check(!heal.isCanceled(), "normal shot must not lock healing despite generic percent damage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void siriusHitPersistsAndExpires(GameTestHelper helper) {
        registerCowAdapter();
        CountingCow target = cow(helper);
        ParticleBolt bolt = ParticleBolt.shoot(helper.getLevel(), target, Vec3.ZERO, 0, 1, 0, 1, 0, 0, false,
                new PenetrationProfile(0, 0, 3, 10), true);
        check(bolt.isSirius(), "sirius marker");

        
        LivingHealEvent active = new LivingHealEvent(target, 1.0F);

        SiriusCombatHandler.onDamage(new LivingDamageEvent(target,
                TranscendDamage.particleBolt(helper.getLevel(), bolt, target), 1.0F));
        SiriusCombatHandler.onHeal(active);
        check(active.isCanceled(), "lock active");

        CompoundTag saved = bolt.saveWithoutId(new CompoundTag());
        check(saved.getBoolean("BoltSirius"), "NBT marker");
        ParticleBolt restored = new ExposedBolt(helper.getLevel(), target);
        ((ExposedBolt) restored).exposedRead(saved);
        check(restored.isSirius(), "NBT marker round trip");
        check(restored.penetration().maxHealthDamagePercent() == 10.0F, "NBT penetration round trip");

        helper.runAfterDelay(41, () -> {
            LivingHealEvent expired = new LivingHealEvent(target, 1.0F);
            SiriusCombatHandler.onHeal(expired);
            check(!expired.isCanceled(), "lock expires after 40 ticks");
            helper.succeed();
        });
    }

    private static CountingCow cow(GameTestHelper helper) {
        CountingCow target = new CountingCow(EntityType.COW, helper.getLevel());
        target.moveTo(0.5, 60, 0.5, 0, 0);
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    /** 作为天狼星战斗测试目标记录受伤调用次数，并供生命值适配器绑定使用。 */
    private static class CountingCow extends Cow {
        int hurtCalls;
        CountingCow(EntityType<Cow> type, net.minecraft.world.level.Level level) { super(type, level); }
        @Override public boolean hurt(DamageSource source, float amount) { hurtCalls++; return super.hurt(source, amount); }
    }

    /** 向游戏测试开放粒子弹受保护的存档读取和实体命中入口。 */
    private static class ExposedBolt extends ParticleBolt {
        ExposedBolt(net.minecraft.world.level.Level level, LivingEntity owner) {
            super(level, owner, Vec3.ZERO, 0, 1);
        }
        void exposedRead(CompoundTag tag) { readAdditionalSaveData(tag); }
        void exposedHit(EntityHitResult hit) { onHitEntity(hit); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
