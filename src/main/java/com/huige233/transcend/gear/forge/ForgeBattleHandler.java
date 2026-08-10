package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 锻造战斗事件处理。 */
public class ForgeBattleHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;
        Level level = victim.level();
        if (level.isClientSide) return;

        float amount = event.getAmount();
        if (amount <= 0) return;

        boolean weaponForged = false;
        boolean critTriggered = false;

        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
            if (!weapon.isEmpty()
                    && GearForgeData.isInPipeline(weapon)
                    && GearCategory.classify(weapon) == GearCategory.WEAPON) {

                weaponForged = true;
                float mult = computeAttackerMultiplier(level, attacker, weapon, victim);
                amount *= clamp(mult, 0.05f, 4.0f);

                int sparkSockets = countSocketsByKind(weapon, ResonanceKind.SPARK);
                if (sparkSockets > 0) {
                    float critChance = sparkSockets * ForgeBattleConfig.SPARK_CRIT_PER_SOCKET;
                    if (level.random.nextFloat() < critChance) {
                        amount *= (1f + ForgeBattleConfig.SPARK_CRIT_BONUS_DAMAGE);
                        critTriggered = true;
                    }
                }

                int leechSockets = countSocketsByKind(weapon, ResonanceKind.LEECH);
                if (leechSockets > 0) {
                    float heal = amount * leechSockets * ForgeBattleConfig.LEECH_HEAL_PER_SOCKET;
                    if (heal > 0) attacker.heal(heal);
                }

                if (level instanceof ServerLevel serverLevel) {
                    if (critTriggered) {
                        ForgeVisualEffects.spawnCritBurst(serverLevel, victim, weapon, attacker);
                    } else {
                        ForgeVisualEffects.spawnHitBurst(serverLevel, victim, weapon);
                    }
                }
            }
        }

        if (victim instanceof Player playerVictim) {
            float defenseMult = computeDefenseMultiplier(level, playerVictim);
            amount *= clamp(defenseMult, 0.10f, 1.0f);

            if (defenseMult < 1.0f && level instanceof ServerLevel serverLevel) {

                ItemStack themedArmor = pickThemedArmor(playerVictim);
                if (!themedArmor.isEmpty()) {
                    ForgeVisualEffects.spawnDefenseAura(serverLevel, playerVictim, themedArmor);
                }
            }
        }

        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onKillWithForgedWeapon(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (!(killer.level() instanceof ServerLevel serverLevel)) return;

        ItemStack weapon = killer.getItemInHand(InteractionHand.MAIN_HAND);
        if (weapon.isEmpty()) return;
        if (!GearForgeData.isInPipeline(weapon)) return;
        if (GearCategory.classify(weapon) != GearCategory.WEAPON) return;

        ForgeVisualEffects.spawnKillExecution(serverLevel, victim, weapon);
    }

    private static ItemStack pickThemedArmor(Player victim) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = victim.getItemBySlot(slot);
            if (!armor.isEmpty()
                    && GearForgeData.isInPipeline(armor)
                    && GearCategory.classify(armor) == GearCategory.ARMOR) {
                return armor;
            }
        }
        return ItemStack.EMPTY;
    }

    private static float computeAttackerMultiplier(Level level, Player attacker,
                                                    ItemStack weapon, LivingEntity victim) {
        float mult = 1.0f;

        GearForgeData.CrucibleData crucible = GearForgeData.getCrucible(weapon);
        if (crucible != null) {
            mult *= (1.0f + crucible.offset());
        }

        int matching = countMatchingEchoes(weapon, victim);
        if (matching > 0) {
            mult *= (1.0f + matching * ForgeBattleConfig.SOUL_ECHO_DAMAGE_BONUS);
        }

        int tier = GearForgeData.getExperience(weapon).tier();
        if (tier > 0 && tier < ForgeBattleConfig.TIER_MULT.length) {
            mult *= (1.0f + ForgeBattleConfig.TIER_MULT[tier]);
        }

        GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(weapon);
        if (bless != null) {
            mult *= computeBlessingAttackerMult(level, bless);
        }

        return mult;
    }

    private static float computeBlessingAttackerMult(Level level, GearForgeData.CelestialBlessing bless) {
        BlessingDef def = BlessingRegistry.byId(bless.blessing());
        if (def == BlessingRegistry.INDETERMINATE) return 1.0f + ForgeBattleConfig.BLESSING_INDETERMINATE_BONUS;

        float base = def.isPure()
                ? ForgeBattleConfig.BLESSING_PURE_BONUS
                : ForgeBattleConfig.BLESSING_DUAL_BONUS;

        if ("solar_crown".equals(def.id()) && level.isDay()) {
            base += ForgeBattleConfig.SOLAR_DAY_BONUS;
        } else if ("lunar_crown".equals(def.id()) && level.isNight()) {
            base += ForgeBattleConfig.LUNAR_NIGHT_BONUS;
        }

        return 1.0f + base;
    }

    private static float computeDefenseMultiplier(Level level, Player victim) {
        float reduction = 0.0f;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = victim.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            if (!GearForgeData.isInPipeline(armor)) continue;
            if (GearCategory.classify(armor) != GearCategory.ARMOR) continue;

            int tier = GearForgeData.getExperience(armor).tier();
            if (tier > 0 && tier < ForgeBattleConfig.TIER_MULT.length) {
                reduction += ForgeBattleConfig.TIER_MULT[tier] * 0.5f;
            }

            GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(armor);
            if (bless != null) {
                BlessingDef def = BlessingRegistry.byId(bless.blessing());
                if (def != BlessingRegistry.INDETERMINATE) {
                    float blessReduction = def.isPure()
                            ? ForgeBattleConfig.BLESSING_PURE_BONUS * 0.25f
                            : ForgeBattleConfig.BLESSING_DUAL_BONUS * 0.25f;
                    reduction += blessReduction;
                }
            }
        }

        reduction = Math.min(0.75f, reduction);
        return 1.0f - reduction;
    }

    private static int countSocketsByKind(ItemStack stack, ResonanceKind target) {
        int n = 0;
        for (GearForgeData.ResonanceSocket socket : GearForgeData.getSockets(stack)) {
            if (target.id.equals(socket.crystalId())) n++;
        }
        return n;
    }

    private static int countMatchingEchoes(ItemStack weapon, LivingEntity victim) {
        ResourceLocation victimId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (victimId == null) return 0;
        String victimKey = victimId.toString();
        int n = 0;
        for (GearForgeData.SoulEcho echo : GearForgeData.getSoulEchoes(weapon)) {
            if (victimKey.equals(echo.mobId())) n++;
        }
        return n;
    }

    private static float clamp(float v, float lo, float hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
