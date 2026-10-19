package com.huige233.transcend;

import com.huige233.transcend.combat.attack.HealthAccess;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.mixin.LivingEntityAttackInvoker;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.TranscendDeadInside;
import com.huige233.transcend.util.TranscendDefense;
import com.huige233.transcend.util.TranscendForceKillUtil;
import com.huige233.transcend.util.TranscendGuard;
import com.huige233.transcend.util.TranscendUnsafe;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.*;

import java.util.UUID;

/** 验证实际 Mixin、同步生命数据及 Forge 总线上的神器攻防行为。 */
@net.minecraftforge.gametest.GameTestHolder(Transcend.MODID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ArtifactCombatGameTests {
    private ArtifactCombatGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void swordRepairsRawHealthAndBlocksDirectDamage(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().items.set(9, new ItemStack(ModItems.transcend_sword.get()));
        check(HealthAccess.find(player).isPresent(), "production player adapter missing");
        check(TranscendGuard.isProtected(player), "inventory sword qualification");
        player.getEntityData().set(TranscendUnsafe.dataHealthId(), Float.NaN);
        TranscendGuard.enforce(player);
        check(HealthAccess.read(player) == player.getMaxHealth(), "raw NaN health was not repaired");
        player.setHealth(3.0F);
        check(HealthAccess.read(player) == player.getMaxHealth(), "finite direct health reduction");
        player.setAbsorptionAmount(7.0F);
        ((LivingEntityAttackInvoker) player).transcend$actuallyHurt(player.damageSources().genericKill(), 100.0F);
        check(player.getAbsorptionAmount() == 7.0F, "Player.actuallyHurt consumed absorption");
        check(HealthAccess.read(player) == player.getMaxHealth(), "direct actuallyHurt reduced health");
        player.remove(Entity.RemovalReason.KILLED);
        check(!player.isRemoved(), "protected player was removed");
        check(!TranscendProtectionEngine.shouldBlockRemoval(player, Entity.RemovalReason.CHANGED_DIMENSION),
                "dimension lifecycle was blocked");
        ServerPlayer disconnected = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "disconnected_test")) {
            @Override public boolean hasDisconnected() { return true; }
        };
        disconnected.getInventory().items.set(0, new ItemStack(ModItems.transcend_sword.get()));
        disconnected.remove(Entity.RemovalReason.DISCARDED);
        check(disconnected.isRemoved(), "logout cleanup was blocked by sword protection");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void armorAndTerminalStateUseSameDecision(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.transcend_helmet.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.transcend_chestplate.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.transcend_leggings.get()));
        check(!TranscendGuard.isProtected(player), "partial armor became invulnerable");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.transcend_boots.get()));
        check(TranscendGuard.isProtected(player), "full armor qualification");
        TranscendDefense.grantReviveInvulnerable(player);
        ((ITranscendMarked) player).transcend$mark();
        check(!TranscendGuard.isProtected(player), "mark must override armor and revive window");
        LivingKnockBackEvent knockback = new LivingKnockBackEvent(player, 1.0F, 1.0, 0.0);
        MinecraftForge.EVENT_BUS.post(knockback);
        check(!knockback.isCanceled(), "marked armor still blocked knockback");
        check(!TranscendProtectionEngine.shouldBlockRemoval(player, Entity.RemovalReason.KILLED),
                "marked armor still blocked removal");
        player.setHealth(2.0F);
        player.heal(5.0F);
        check(HealthAccess.read(player) == 2.0F, "marked player healed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void reviveWindowProtectsEveryEntryAndExpires(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        TranscendDefense.grantReviveInvulnerable(player);
        player.setHealth(Float.NEGATIVE_INFINITY);
        player.die(player.damageSources().genericKill());
        player.remove(Entity.RemovalReason.DISCARDED);
        check(!player.isRemoved() && HealthAccess.read(player) == player.getMaxHealth(), "revive protection split");
        player.getPersistentData().putInt(TranscendDefense.NBT_INVULNERABLE_TIME, 1);
        TranscendDefense.tickInvulnerable(player);
        check(!TranscendGuard.isProtected(player), "expired window remained protected");
        player.setHealth(3.0F);
        check(HealthAccess.read(player) == 3.0F, "expired window still locked health");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void terminalEventsReceiveAlreadyCanceledEvents(GameTestHelper helper) {
        Cow cow = new Cow(EntityType.COW, helper.getLevel());
        ((ITranscendMarked) cow).transcend$mark();
        DamageSource source = cow.damageSources().genericKill();
        LivingAttackEvent attack = new LivingAttackEvent(cow, source, 1.0F);
        LivingHurtEvent hurt = new LivingHurtEvent(cow, source, 1.0F);
        LivingDamageEvent damage = new LivingDamageEvent(cow, source, 1.0F);
        LivingDeathEvent death = new LivingDeathEvent(cow, source);
        for (var event : new net.minecraftforge.eventbus.api.Event[]{attack, hurt, damage, death}) {
            event.setCanceled(true);
            MinecraftForge.EVENT_BUS.post(event);
            check(!event.isCanceled(), "terminal listener missed canceled " + event.getClass().getSimpleName());
        }
        check(hurt.getAmount() == Float.MAX_VALUE && damage.getAmount() == Float.MAX_VALUE, "terminal damage amount");
        check(TranscendProtectionEngine.blocksTotem(cow), "non-player terminal state was lost");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void markedHurtCannotReenterFromActuallyHurt(GameTestHelper helper) {
        ReentrantCow cow = new ReentrantCow(helper.getLevel());
        ((ITranscendMarked) cow).transcend$mark();
        cow.hurt(cow.damageSources().genericKill(), 1.0F);
        check(cow.calls == 1, "terminal hurt recursively reentered");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void forceKillPreservesExistingMarkAndAllowsLaterCalls(GameTestHelper helper) {
        Cow marked = new Cow(EntityType.COW, helper.getLevel());
        ((ITranscendMarked) marked).transcend$mark();
        TranscendForceKillUtil.forceKillLiving(marked, null);
        check(((ITranscendMarked) marked).transcend$isMarked(), "existing mark was cleared by finally");
        Cow unmarked = new Cow(EntityType.COW, helper.getLevel());
        TranscendForceKillUtil.forceKillLiving(unmarked, null);
        check(!((ITranscendMarked) unmarked).transcend$isMarked(), "temporary kill mark leaked");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void forceKillCannotReenterFromEffectCallbacks(GameTestHelper helper) {
        CallbackCow cow = new CallbackCow(helper.getLevel());
        TranscendForceKillUtil.forceKillLiving(cow, null);
        check(cow.calls > 0, "effect callback did not run");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void adapterWriteDoesNotInvokeOverriddenSetter(GameTestHelper helper) {
        SetterTrapCow cow = new SetterTrapCow(helper.getLevel());
        HealthAccess.register(SetterTrapCow.class, new HealthAccess.Adapter<SetterTrapCow>() {
            public double get(SetterTrapCow entity) { return entity.externalHealth; }
            public boolean set(SetterTrapCow entity, double value) {
                entity.externalHealth = (float) value;
                return true;
            }
        });
        check(HealthAccess.write(cow, 7.0F), "adapter write rejected");
        check(HealthAccess.read(cow) == 7.0F && cow.getHealth() == 7.0F, "adapter or vanilla display not synchronized");
        check(cow.setterCalls == 0, "adapter result was overwritten by setHealth");
        TranscendForceKillUtil.forceKillLiving(cow, null);
        check(cow.externalHealth == 0.0F, "sword kill did not use external health adapter");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void deadInsideBlocksProtectionAndHealing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().items.set(0, new ItemStack(ModItems.transcend_sword.get()));
        TranscendDeadInside.antiResurrect(player, 60_000L);
        check(!TranscendGuard.isProtected(player), "dead-inside target kept sword immunity");
        player.setHealth(2.0F);
        player.heal(5.0F);
        check(HealthAccess.read(player) == 2.0F, "dead-inside target healed");
        player.getPersistentData().remove(TranscendDeadInside.NBT_DEAD_INSIDE);
        check(TranscendGuard.isProtected(player), "cleared anti-revive state did not restore qualification");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "artifact_test"));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class ReentrantCow extends Cow {
        int calls;
        ReentrantCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override protected void actuallyHurt(DamageSource source, float amount) {
            if (++calls > 1) throw new AssertionError("recursive actuallyHurt");
            hurt(source, amount);
            super.actuallyHurt(source, amount);
        }
    }

    private static class CallbackCow extends Cow {
        int calls;
        CallbackCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override public boolean removeAllEffects() {
            if (++calls == 1) {
                TranscendForceKillUtil.forceKillLiving(this, null);
                check(calls == 1, "force kill reentered its own effect callback");
            }
            return super.removeAllEffects();
        }
    }

    private static class SetterTrapCow extends Cow {
        float externalHealth = 10.0F;
        int setterCalls;
        SetterTrapCow(ServerLevel level) {
            super(EntityType.COW, level);
            setterCalls = 0;
        }
        @Override public void setHealth(float value) {
            setterCalls++;
            externalHealth = 42.0F;
        }
    }
}
