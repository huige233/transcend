package com.huige233.transcend;

import com.huige233.transcend.combat.attack.TranscendKillSettlement;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.mixin.EntityLifecycleAccessor;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.*;
import com.huige233.transcend.handle.TranscendKillHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/** 验证服务端实体索引、存档终结状态和幂等击杀结算。 */
@net.minecraftforge.gametest.GameTestHolder(Transcend.MODID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ArtifactLifecycleGameTests {
    private ArtifactLifecycleGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void directWorldCallbackCannotRemoveSwordPlayer(GameTestHelper helper) {
        FakePlayer player = indexedPlayer(helper);
        try {
            EntityInLevelCallback callback = ((EntityLifecycleAccessor) player).transcend$getLevelCallback();
            check(callback != EntityInLevelCallback.NULL, "real manager callback missing");
            for (Entity.RemovalReason reason : new Entity.RemovalReason[]{
                    Entity.RemovalReason.KILLED, Entity.RemovalReason.DISCARDED}) {
                callback.onRemove(reason);
                check(helper.getLevel().getEntity(player.getId()) == player, "ID index was stripped");
                check(helper.getLevel().getEntity(player.getUUID()) == player, "UUID index was stripped");
                check(helper.getLevel().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox()).contains(player),
                        "spatial section was stripped");
            }
            ((ITranscendMarked) player).transcend$mark();
            callback.onRemove(Entity.RemovalReason.DISCARDED);
            check(!TranscendForceKillUtil.isPersisting(player), "terminal state did not permit callback cleanup");
        } finally {
            player.getInventory().clearContent();
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void worldCallbackStillAllowsDimensionLifecycle(GameTestHelper helper) {
        FakePlayer player = indexedPlayer(helper);
        try {
            ((EntityLifecycleAccessor) player).transcend$getLevelCallback()
                    .onRemove(Entity.RemovalReason.CHANGED_DIMENSION);
            check(!TranscendForceKillUtil.isPersisting(player), "dimension callback was blocked");
        } finally {
            player.getInventory().clearContent();
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void deadFlagsDoNotHideResidualWorldIndexes(GameTestHelper helper) {
        Cow cow = new Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0, 0);
        check(helper.getLevel().addFreshEntity(cow), "cow was not indexed");
        cow.setHealth(0);
        TranscendUnsafe.putMcObject(cow, Entity.class, TranscendUnsafe.SRG_REMOVAL_REASON,
                Entity.RemovalReason.KILLED);
        check(cow.isRemoved() && !cow.isAlive(), "residual state setup");
        check(TranscendForceKillUtil.isPersisting(cow), "dead flags hid a live world index");
        TranscendForceKillUtil.forceRemove(cow, Entity.RemovalReason.KILLED);
        check(!TranscendForceKillUtil.isPersisting(cow), "residual indexes survived cleanup");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void explicitTerminalMarkSurvivesSaveAndLoad(GameTestHelper helper) {
        Cow cow = new Cow(EntityType.COW, helper.getLevel());
        ((ITranscendMarked) cow).transcend$mark();
        CompoundTag saved = cow.saveWithoutId(new CompoundTag());
        Cow loaded = new Cow(EntityType.COW, helper.getLevel());
        loaded.load(saved);
        check(((ITranscendMarked) loaded).transcend$isMarked(), "explicit mark lost on load");
        check(TranscendProtectionEngine.blocksHealing(loaded), "loaded terminal mark did not block healing");
        ((ITranscendMarked) loaded).transcend$unmark();
        Cow cleared = new Cow(EntityType.COW, helper.getLevel());
        cleared.load(loaded.saveWithoutId(new CompoundTag()));
        check(!((ITranscendMarked) cleared).transcend$isMarked(), "cleared mark was saved again");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void transientKillContextIsNotSavedAsPermanentMark(GameTestHelper helper) {
        SnapshotCow cow = new SnapshotCow(helper.getLevel());
        TranscendForceKillUtil.forceKillLiving(cow, null);
        check(cow.snapshot != null, "kill callback did not save a snapshot");
        Cow loaded = new Cow(EntityType.COW, helper.getLevel());
        loaded.load(cow.snapshot);
        check(!((ITranscendMarked) loaded).transcend$isMarked(), "temporary mark leaked into ForgeData");
        check(!TranscendProtectionEngine.isTerminal(loaded), "temporary kill context leaked across instances");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void deadInsideCloneRetainsDeadlineAndExpires(GameTestHelper helper) {
        ServerPlayer old = player(helper), clone = player(helper), expiredClone = player(helper);
        TranscendDeadInside.antiResurrect(old, 60_000L);
        long until = old.getPersistentData().getLong(TranscendDeadInside.NBT_DEAD_INSIDE);
        TranscendKillHandler.onClone(new PlayerEvent.Clone(clone, old, true));
        check(clone.getPersistentData().getLong(TranscendDeadInside.NBT_DEAD_INSIDE) == until,
                "respawn lost or extended the original deadline");
        check(TranscendProtectionEngine.isTerminal(clone), "clone escaped active anti-revival");
        old.getPersistentData().putLong(TranscendDeadInside.NBT_DEAD_INSIDE, System.currentTimeMillis() - 1);
        TranscendKillHandler.onClone(new PlayerEvent.Clone(expiredClone, old, true));
        check(!TranscendProtectionEngine.isTerminal(expiredClone), "expired anti-revival copied to clone");
        TranscendDeadInside.antiResurrect(old, Long.MAX_VALUE);
        check(TranscendDeadInside.isAntiResurrect(old), "long duration overflowed into expiration");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void forceKillSettlesCreditAndLootOnce(GameTestHelper helper) {
        SettlementCow cow = new SettlementCow(helper.getLevel());
        ServerPlayer attacker = player(helper);
        TranscendForceKillUtil.forceKillLiving(cow, attacker);
        TranscendForceKillUtil.forceKillLiving(cow, attacker);
        check(cow.lootCalls == 1 && cow.experienceCalls == 1, "repeat force kill duplicated loot or experience");
        check(cow.recentPlayerHit, "player kill window was lost before loot");
        check(cow.getKillCredit() == attacker, "force kill lost attacker credit");
        check(TranscendKillSettlement.hasSettledLoot(cow), "settlement was not recorded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void overriddenDeathStillGetsOneFallbackSettlement(GameTestHelper helper) {
        SettlementCow cow = new SettlementCow(helper.getLevel()) {
            @Override public void die(DamageSource source) { }
        };
        ServerPlayer attacker = player(helper);
        TranscendForceKillUtil.forceKillLiving(cow, attacker);
        TranscendForceKillUtil.forceKillLiving(cow, attacker);
        check(cow.lootCalls == 1 && cow.experienceCalls == 1, "fallback settlement missing or duplicated");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void healthWriteDoesNotGuessOtherSyncedFloats(GameTestHelper helper) {
        HealthDataCow cow = new HealthDataCow(helper.getLevel());
        cow.getEntityData().set(HealthDataCow.ENERGY, cow.getMaxHealth());
        TranscendUnsafeKill.catchSetTrueHealth(cow, Float.NEGATIVE_INFINITY);
        check(cow.getEntityData().get(TranscendUnsafe.dataHealthId()) == 0.0F, "raw health is not finite zero");
        check(cow.getEntityData().get(HealthDataCow.ENERGY) == cow.getMaxHealth(), "unrelated energy was treated as HP");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void repairCommandClearsPersistentTerminalState(GameTestHelper helper) throws Exception {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "repair_test")) {
            @Override public void sendSystemMessage(Component message) { }
        };
        ((ITranscendMarked) player).transcend$mark();
        TranscendDeadInside.antiResurrect(player, 60_000L);
        var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        int result = helper.getLevel().getServer().getCommands().getDispatcher().execute("tr_fix", source);
        check(result == 1 && !TranscendProtectionEngine.isTerminal(player), "repair command did not clear terminal state");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "settlement_test"));
    }

    private static FakePlayer indexedPlayer(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "indexed_test"));
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0, 0);
        player.getInventory().items.set(0, new ItemStack(ModItems.transcend_sword.get()));
        check(helper.getLevel().addFreshEntity(player), "player could not be indexed");
        return player;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class SettlementCow extends Cow {
        int lootCalls, experienceCalls;
        boolean recentPlayerHit;
        SettlementCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override protected void dropFromLootTable(DamageSource source, boolean recent) { }
        @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recent) {
            lootCalls++;
            recentPlayerHit = recent;
        }
        @Override protected void dropExperience() { experienceCalls++; }
    }

    private static class SnapshotCow extends Cow {
        CompoundTag snapshot;
        SnapshotCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override public boolean removeAllEffects() {
            if (snapshot == null) snapshot = saveWithoutId(new CompoundTag());
            return super.removeAllEffects();
        }
    }

    private static class HealthDataCow extends Cow {
        static final EntityDataAccessor<Float> ENERGY = SynchedEntityData.defineId(HealthDataCow.class, EntityDataSerializers.FLOAT);
        HealthDataCow(ServerLevel level) { super(EntityType.COW, level); }
        @Override protected void defineSynchedData() {
            super.defineSynchedData();
            getEntityData().define(ENERGY, 20.0F);
        }
    }
}
