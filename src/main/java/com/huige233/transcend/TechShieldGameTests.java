package com.huige233.transcend;

import com.huige233.transcend.entity.TestDummy.DamageCategory;
import com.huige233.transcend.handle.tech.PhaseShieldHandler;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.tech.PhaseShield;
import com.huige233.transcend.items.tech.PhaseShieldModules;
import com.huige233.transcend.items.tech.ShieldModule;
import com.huige233.transcend.items.tech.ShieldModuleItem;
import com.huige233.transcend.tech.combat.TechSuperAttackResolver;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.quality.TechQuality;
import com.huige233.transcend.tech.rank.TechRank;
import com.huige233.transcend.tech.shield.ShieldDamageContext;
import com.huige233.transcend.tech.shield.ShieldResolution;
import com.huige233.transcend.tech.shield.TechShieldTierProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

/** 在真实注册表与服务端事件环境中验证科技护盾的有限能源、等级边界和生命攻击拦截。 */
@net.minecraftforge.gametest.GameTestHolder(Transcend.MODID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class TechShieldGameTests {
    private TechShieldGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rankedShieldsAndModulesHaveLoadedRecipes(GameTestHelper helper) {
        for (TechRank rank : TechRank.values()) {
            var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(Transcend.MODID, "phase_shield_" + rank.id())).orElseThrow();
            check(TechItemData.getRank(recipe.getResultItem(helper.getLevel().registryAccess())) == rank, "recipe preserves shield rank");
        }
        for (ShieldModule module : ShieldModule.values()) {
            var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(Transcend.MODID, "shield_module_" + module.id())).orElseThrow();
            check(ShieldModuleItem.getModule(recipe.getResultItem(helper.getLevel().registryAccess())) == module, "recipe preserves module type");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void moduleSlotsAndRanksAreEnforced(GameTestHelper helper) {
        for (TechRank rank : new TechRank[]{TechRank.E, TechRank.D, TechRank.C}) {
            check(!install(shield(rank), ShieldModule.BASIC), rank + " cannot install modules");
        }
        ItemStack shield = shield(TechRank.B);
        check(!install(shield, ShieldModule.A_OMNI), "B cannot install A module");
        check(install(shield, ShieldModule.B_MAGIC), "first B module");
        check(!install(shield, ShieldModule.B_MAGIC), "no duplicate family");
        check(install(shield, ShieldModule.B_PROJECTILE), "second B module");
        check(!install(shield, ShieldModule.EFFICIENT), "two B slots full");
        TechItemData.setRank(shield, TechRank.C);
        check(PhaseShieldModules.activeModules(shield).isEmpty(), "downgrade disables installed modules");
        check(!PhaseShield.covers(shield, DamageCategory.MAGIC.index), "inactive module cannot add magic coverage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void modulesRoundTripWithoutDuplicationOrFreeCharge(GameTestHelper helper) {
        ItemStack shield = shield(TechRank.B);
        PhaseShield.drainCapacitor(shield, 100);
        float charge = PhaseShield.getEnergy(shield);
        ItemStack old = ShieldModuleItem.create(ShieldModule.EFFICIENT);
        old.setHoverName(Component.literal("saved component"));
        old.getOrCreateTag().putInt("CustomValue", 37);
        shield.getOrCreateTag().putString(PhaseShieldModules.MODULE, ShieldModule.EFFICIENT.id());
        shield.getOrCreateTag().put(PhaseShieldModules.INSTALLED, old.save(new CompoundTag()));
        check(install(shield, ShieldModule.B_MAGIC), "append alongside legacy component");
        check(PhaseShieldModules.activeModules(shield).size() == 2, "legacy and new component active");
        near(PhaseShield.getEnergy(shield), charge, "install does not recharge");
        check(ShieldModuleItem.getModule(PhaseShieldModules.removeLast(shield)) == ShieldModule.B_MAGIC, "last component removed");
        check(ItemStack.isSameItemSameTags(old, PhaseShieldModules.removeLast(shield)), "original component NBT restored");
        check(PhaseShieldModules.removeLast(shield).isEmpty(), "cannot remove twice");
        near(PhaseShield.getEnergy(shield), charge, "uninstall does not recharge");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void specializationAndHighRankCoverage(GameTestHelper helper) {
        ItemStack b = shield(TechRank.B);
        check(!PhaseShield.absorb(b, hit(DamageCategory.MAGIC, 20, 0, null)).absorbedAny(), "B base excludes magic");
        check(install(b, ShieldModule.B_MAGIC), "magic specialization installed");
        check(PhaseShield.absorb(b, hit(DamageCategory.MAGIC, 20, 0, null)).fullyAbsorbed(), "B specialization covers magic");
        check(!PhaseShield.covers(b, DamageCategory.ENVIRONMENT.index), "magic specialization does not cover environment");
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.OFF_HAND, b);
        player.startUsingItem(InteractionHand.OFF_HAND);
        LivingHurtEvent magic = new LivingHurtEvent(player, player.damageSources().magic(), 20);
        PhaseShieldHandler.onPhaseShieldHurt(magic);
        near(magic.getAmount(), 0, "magic bypasses vanilla shield but not specialized phase field");
        for (TechRank rank : new TechRank[]{TechRank.S, TechRank.EX}) {
            ItemStack high = shield(rank);
            for (DamageCategory category : DamageCategory.values()) {
                check(PhaseShield.absorb(high, hit(category, 10, 0, null)).fullyAbsorbed(), rank + " covers " + category);
            }
            check(PhaseShield.absorb(high, hit(null, 10, 0, null)).fullyAbsorbed(), rank + " covers unknown damage");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sustainedReferenceFireAndPenetration(GameTestHelper helper) {
        int[] seconds = {10, 20, 30, 60, 120, 180, 240};
        for (TechRank rank : TechRank.values()) {
            ItemStack shield = shield(rank);
            TechShieldTierProfile profile = TechShieldTierProfile.of(rank);
            // 参考输入：每秒两发，各档使用 profile 的标准伤害；A 以上附加生命层预算。
            float damage = profile.referenceShotDamage() + 20 * rank.directHealthPercent() / 100;
            for (int i = 0; i < seconds[rank.ordinal()] * 2; i++) {
                check(PhaseShield.absorb(shield, hit(DamageCategory.PROJECTILE, damage, 0, rank)).fullyAbsorbed(),
                        rank + " must sustain reference fire for " + seconds[rank.ordinal()] + " seconds");
            }
            check(PhaseShield.getEnergy(shield) > 0, "finite energy remains after benchmark");
            check(PhaseShield.absorb(shield, hit(DamageCategory.PROJECTILE, damage, 999, rank)).fullyAbsorbed(),
                    "same-rank piercing increases drain instead of skipping field");
        }
        ItemStack b = shield(TechRank.B);
        check(!PhaseShield.absorb(b, hit(DamageCategory.PROJECTILE, 30, 999, TechRank.EX)).absorbedAny(),
                "higher-rank weapon can penetrate lower-rank field");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void chargeSurvivesSaveAndCannotRegenerateForFree(GameTestHelper helper) {
        ItemStack shield = shield(TechRank.A);
        float initial = PhaseShield.getEnergy(shield);
        ShieldResolution result = PhaseShield.absorb(shield, hit(DamageCategory.PROJECTILE, 100, 0, TechRank.A));
        near(PhaseShield.getEnergy(shield), initial - result.shieldEnergySpent(), "charge actually drained");
        ItemStack restored = ItemStack.of(shield.save(new CompoundTag()));
        PhaseShield.initialize(restored);
        near(PhaseShield.getEnergy(restored), PhaseShield.getEnergy(shield), "save does not recharge");
        PhaseShield.drainCapacitor(restored, Float.MAX_VALUE);
        PhaseShield.initialize(restored);
        PhaseShield.tickRegen(restored);
        check(!PhaseShield.absorb(restored, hit(DamageCategory.PROJECTILE, 10, 0, TechRank.A)).absorbedAny(), "empty shield cannot absorb");
        check(install(restored, ShieldModule.A_OMNI), "module installs into empty shield");
        near(PhaseShield.getEnergy(restored), 0, "module cannot recharge empty shield");
        TechItemData.setRank(restored, TechRank.EX);
        PhaseShield.initialize(restored);
        near(PhaseShield.getEnergy(restored), 0, "rank upgrade cannot create charge");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void directHealthAttackChecksShieldAndShotBudget(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.getAbilities().invulnerable = false;
        ItemStack shield = shield(TechRank.EX);
        player.setItemInHand(InteractionHand.OFF_HAND, shield);
        player.startUsingItem(InteractionHand.OFF_HAND);
        UUID shot = UUID.randomUUID();
        float health = player.getHealth();
        float charge = PhaseShield.getEnergy(shield);
        long tick = helper.getLevel().getGameTime();
        check(TechSuperAttackResolver.apply(player, TechRank.EX, TechQuality.MYTHIC, true, shot, tick), "EX attack settled");
        near(player.getHealth(), health, "charged field prevents direct life write");
        check(PhaseShield.getEnergy(shield) < charge, "direct life attack consumes charge");
        float after = PhaseShield.getEnergy(shield);
        check(!TechSuperAttackResolver.apply(player, TechRank.EX, TechQuality.MYTHIC, true, shot, tick + 20), "same shot stays spent past 20 ticks");
        near(PhaseShield.getEnergy(shield), after, "duplicate cannot drain field again");
        PhaseShield.drainCapacitor(shield, Float.MAX_VALUE);
        check(TechSuperAttackResolver.apply(player, TechRank.EX, TechQuality.MYTHIC, true, UUID.randomUUID(), tick + 21), "unshielded EX attack");
        check(player.getHealth() < health, "exhausted shield allows direct life damage");
        var weakTarget = new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW, helper.getLevel());
        weakTarget.setHealth(0.5F);
        TechSuperAttackResolver.apply(weakTarget, TechRank.A, TechQuality.COMMON, false, UUID.randomUUID(), tick + 21);
        near(weakTarget.getHealth(), 0.5F, "A health floor must not heal targets already below one health");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void oxygenUsesFiniteShieldEnergy(GameTestHelper helper) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "oxygen-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
            @Override public boolean isUnderWater() { return true; }
        };
        ItemStack shield = shield(TechRank.B);
        float before = PhaseShield.getEnergy(shield);
        player.setAirSupply(10);
        check(PhaseShield.supplyOxygen(shield, player), "powered supply works underwater");
        near(player.getAirSupply(), player.getMaxAirSupply(), "oxygen restored");
        near(PhaseShield.getEnergy(shield), before - PhaseShield.OXYGEN_ENERGY_PER_TICK, "oxygen costs energy");
        PhaseShield.drainCapacitor(shield, Float.MAX_VALUE);
        player.setAirSupply(10);
        check(!PhaseShield.supplyOxygen(shield, player), "empty supply stops");
        near(player.getAirSupply(), 10, "empty shield cannot refresh oxygen");
        helper.succeed();
    }

    private static ItemStack shield(TechRank rank) {
        ItemStack stack = new ItemStack(ModItems.phase_shield.get());
        TechItemData.setRank(stack, rank);
        PhaseShield.initialize(stack);
        return stack;
    }

    private static ShieldDamageContext hit(DamageCategory category, float amount, int strength, TechRank rank) {
        return new ShieldDamageContext(category, amount, false, strength, rank);
    }

    private static boolean install(ItemStack shield, ShieldModule module) {
        return PhaseShieldModules.install(shield, module, ShieldModuleItem.create(module).save(new CompoundTag()));
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.002F, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
