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

/**
 * R87: 造物之道战斗 hook 整合 — 读取 R82-R86 写入的 NBT 并应用到战斗事件。
 *
 * <h2>攻击路径（attacker 主手 WEAPON）</h2>
 * <ol>
 *   <li>aspect.offset（E 坩埚 24 aspect）→ 整体 multiplier</li>
 *   <li><s>sharpness sockets × 5%</s> — <b>R90 已迁移至 {@link ForgeAttributeProvider}（ATTACK_DAMAGE）</b></li>
 *   <li>soul echoes 与 victim 同 mob → ×(1 + 25% × matchingEchoes)</li>
 *   <li>experience tier × 5/12/25%（C 觉醒）→ damage</li>
 *   <li>celestial blessing（D 加冕）→ +30% pure / +15% dual + 条件加成（solar/lunar）</li>
 *   <li>spark sockets × 2% 暴击率 → 触发时 ×1.5</li>
 *   <li>leech sockets × 2% → 击中后治疗攻击者（在 onLivingHurt 直接 heal）</li>
 * </ol>
 *
 * <h2>防御路径（victim 是 Player，4 件 ARMOR）</h2>
 * 每件穿戴的 ARMOR-class 已 forge 的装备：
 * <ol>
 *   <li><s>ward sockets × 3%</s> — <b>R90 已迁移至 {@link ForgeAttributeProvider}（ARMOR）</b>；vanilla armor formula 接管</li>
 *   <li>experience tier × 5/12/25% × 0.5 → 减伤（armor 收益减半，避免叠加无敌）</li>
 *   <li>celestial blessing → 减伤补正</li>
 * </ol>
 *
 * <h2>R90 解耦说明</h2>
 * 4 类 socket（SHARPNESS / WARD / SWIFTNESS / FOCUS）现在通过 vanilla AttributeModifier 实施：
 * <ul>
 *   <li>玩家可在 vanilla tooltip 直接看到具体加值（绿色 +X 攻击力 / 紫色 ×5% 等）</li>
 *   <li>与本 handler 的"百分比综合乘数 / 条件加成 / 概率事件"形成清晰分工</li>
 *   <li>SPARK（暴击）与 LEECH（吸血）仍留在本 handler — 这两类无法用 attribute 实现</li>
 * </ul>
 *
 * <h2>整体上限</h2>
 * 任何最终乘数都用 `clamp(0.05, 4.0)` 兜底，防止数据腐败导致负伤害或无限值。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

        // ── 攻击者侧（仅 player 主手 WEAPON）─────────────────────────
        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
            if (!weapon.isEmpty()
                    && GearForgeData.isInPipeline(weapon)
                    && GearCategory.classify(weapon) == GearCategory.WEAPON) {

                weaponForged = true;
                float mult = computeAttackerMultiplier(level, attacker, weapon, victim);
                amount *= clamp(mult, 0.05f, 4.0f);

                // 暴击（spark）
                int sparkSockets = countSocketsByKind(weapon, ResonanceKind.SPARK);
                if (sparkSockets > 0) {
                    float critChance = sparkSockets * ForgeBattleConfig.SPARK_CRIT_PER_SOCKET;
                    if (level.random.nextFloat() < critChance) {
                        amount *= (1f + ForgeBattleConfig.SPARK_CRIT_BONUS_DAMAGE);
                        critTriggered = true;
                    }
                }

                // 吸血（leech）— 攻击者治疗
                int leechSockets = countSocketsByKind(weapon, ResonanceKind.LEECH);
                if (leechSockets > 0) {
                    float heal = amount * leechSockets * ForgeBattleConfig.LEECH_HEAL_PER_SOCKET;
                    if (heal > 0) attacker.heal(heal);
                }

                // R88 视觉特效
                if (level instanceof ServerLevel serverLevel) {
                    if (critTriggered) {
                        ForgeVisualEffects.spawnCritBurst(serverLevel, victim, weapon, attacker);
                    } else {
                        ForgeVisualEffects.spawnHitBurst(serverLevel, victim, weapon);
                    }
                }
            }
        }

        // ── 防御者侧（victim 是 Player，遍历 4 ARMOR 槽）──────────────
        if (victim instanceof Player playerVictim) {
            float defenseMult = computeDefenseMultiplier(level, playerVictim);
            amount *= clamp(defenseMult, 0.10f, 1.0f);

            // R88: 防御特效（仅在有已锻护甲减伤生效时触发，避免每次受击都喷）
            if (defenseMult < 1.0f && level instanceof ServerLevel serverLevel) {
                // 找一件已锻护甲喷主题色（多件以胸甲优先）
                ItemStack themedArmor = pickThemedArmor(playerVictim);
                if (!themedArmor.isEmpty()) {
                    ForgeVisualEffects.spawnDefenseAura(serverLevel, playerVictim, themedArmor);
                }
            }
        }

        event.setAmount(amount);
    }

    /**
     * R88: 已锻武器击杀目标 → 处决特效。
     * 与 R85 ExperienceAwakeningHandler.onKill 同事件但不冲突（一个加经验、一个出特效）。
     */
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

    /** 在 4 个 armor 槽中找一件已锻护甲（优先胸甲，否则按 head→chest→legs→feet 顺序）。 */
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

    // ─── 攻击者综合乘数 ─────────────────────────────────────────────

    private static float computeAttackerMultiplier(Level level, Player attacker,
                                                    ItemStack weapon, LivingEntity victim) {
        float mult = 1.0f;

        // ① aspect offset（E 坩埚）
        GearForgeData.CrucibleData crucible = GearForgeData.getCrucible(weapon);
        if (crucible != null) {
            mult *= (1.0f + crucible.offset());
        }

        // ② sharpness sockets（B 共鸣）— R90: 已迁移至 ForgeAttributeProvider → ATTACK_DAMAGE
        //    此处不再叠乘，避免双重生效

        // ③ soul echoes vs victim mobId（A 注魂）
        int matching = countMatchingEchoes(weapon, victim);
        if (matching > 0) {
            mult *= (1.0f + matching * ForgeBattleConfig.SOUL_ECHO_DAMAGE_BONUS);
        }

        // ④ experience tier（C 觉醒）
        int tier = GearForgeData.getExperience(weapon).tier();
        if (tier > 0 && tier < ForgeBattleConfig.TIER_MULT.length) {
            mult *= (1.0f + ForgeBattleConfig.TIER_MULT[tier]);
        }

        // ⑤ celestial blessing（D 加冕）
        GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(weapon);
        if (bless != null) {
            mult *= computeBlessingAttackerMult(level, bless);
        }

        return mult;
    }

    /** blessing 攻击者乘数（pure +30%, dual +15%, 条件 solar/lunar 再 +20%）。 */
    private static float computeBlessingAttackerMult(Level level, GearForgeData.CelestialBlessing bless) {
        BlessingDef def = BlessingRegistry.byId(bless.blessing());
        if (def == BlessingRegistry.INDETERMINATE) return 1.0f + ForgeBattleConfig.BLESSING_INDETERMINATE_BONUS;

        float base = def.isPure()
                ? ForgeBattleConfig.BLESSING_PURE_BONUS
                : ForgeBattleConfig.BLESSING_DUAL_BONUS;

        // 条件加成
        if ("solar_crown".equals(def.id()) && level.isDay()) {
            base += ForgeBattleConfig.SOLAR_DAY_BONUS;
        } else if ("lunar_crown".equals(def.id()) && level.isNight()) {
            base += ForgeBattleConfig.LUNAR_NIGHT_BONUS;
        }

        return 1.0f + base;
    }

    // ─── 防御者综合乘数（victim 是 Player）─────────────────────────

    private static float computeDefenseMultiplier(Level level, Player victim) {
        float reduction = 0.0f; // 累积减伤百分比（0..1）

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = victim.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            if (!GearForgeData.isInPipeline(armor)) continue;
            if (GearCategory.classify(armor) != GearCategory.ARMOR) continue;

            // ward sockets — R90: 已迁移至 ForgeAttributeProvider → ARMOR
            //   此处不再叠加减伤，避免双重生效（vanilla armor formula 接管）

            // tier 减伤（半价）
            int tier = GearForgeData.getExperience(armor).tier();
            if (tier > 0 && tier < ForgeBattleConfig.TIER_MULT.length) {
                reduction += ForgeBattleConfig.TIER_MULT[tier] * 0.5f;
            }

            // blessing：每件护甲的 blessing 各自贡献小幅减伤
            GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(armor);
            if (bless != null) {
                BlessingDef def = BlessingRegistry.byId(bless.blessing());
                if (def != BlessingRegistry.INDETERMINATE) {
                    float blessReduction = def.isPure()
                            ? ForgeBattleConfig.BLESSING_PURE_BONUS * 0.25f   // 30% × 0.25 = 7.5%
                            : ForgeBattleConfig.BLESSING_DUAL_BONUS * 0.25f;  // 15% × 0.25 = 3.75%
                    reduction += blessReduction;
                }
            }
        }

        // 减伤上限：最多 75% 减伤（即受 25% 伤害）— 防止无敌
        reduction = Math.min(0.75f, reduction);
        return 1.0f - reduction;
    }

    // ─── 工具方法 ────────────────────────────────────────────────────

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
