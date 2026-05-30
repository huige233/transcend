package com.huige233.transcend.ascension;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.tree.PassiveEffect;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * 飞升天赋效果处理器 v2
 *
 * 职责：
 *  1. 击杀/施法计数 → 更新进度
 *  2. 飞升等级/XP 管理 → 升级通知
 *  3. 天赋节点被动效果（伤害/防御/回复）
 *  4. 将合并后的属性块注入玩家 Attribute（HP上限、速度）
 *  5. 元素专精效果（伤害在 TranscendWand/SpellProjectile 中直接查询）
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AscensionHandler {

    // UUID 用于注入/移除 AttributeModifier，保持唯一
    private static final UUID UUID_HP    = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000001");
    private static final UUID UUID_SPEED = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000002");
    private static final UUID UUID_SPELL_POWER   = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000003");
    private static final UUID UUID_SPELL_RESIST  = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000004");
    private static final UUID UUID_MAX_MANA      = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000005");
    private static final UUID UUID_MANA_REGEN    = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000006");
    private static final UUID UUID_CRIT_CHANCE   = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000007");
    private static final UUID UUID_CDR           = UUID.fromString("a5c1e7b3-1234-5678-abcd-000000000008");

    // XP 获取量
    private static final long XP_KILL_NORMAL = 5;
    private static final long XP_KILL_BOSS   = 500;
    public  static final long XP_RITUAL      = 1000;

    // ─── 击杀事件 ─────────────────────────────────────────────────────────

    /**
     * R77: 计算单次击杀的灵魂能（Soul Currency）。
     * <ul>
     *   <li>Boss（{@code isBoss} flag 或 maxHealth ≥ 200）: <b>200</b></li>
     *   <li>Mini-boss（maxHealth ≥ 100）: <b>50</b></li>
     *   <li>Elite（maxHealth ≥ 30）: <b>5</b></li>
     *   <li>普通: <b>1</b></li>
     * </ul>
     * 实际入账受玩家当前 stage 上限截断（见 {@link PlayerAscensionData#getMaxSoulEnergy}）。
     */
    private static long computeSoulGain(LivingEntity target, boolean isBoss) {
        if (isBoss) return 200L;
        float maxHp = target.getMaxHealth();
        if (maxHp >= 100f) return 50L;
        if (maxHp >= 30f) return 5L;
        return 1L;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity target = event.getEntity();
        boolean isBoss = target.getMaxHealth() >= 200f;

        PlayerAscensionData data = AscensionCapability.get(player);

        data.addKill(isBoss);

        // R77: 灵魂能（Soul Currency）— 按目标最大生命分档
        long soulGain = computeSoulGain(target, isBoss);
        data.addSoulEnergy(soulGain);

        long xp = isBoss ? XP_KILL_BOSS : XP_KILL_NORMAL;
        List<PassiveEffect> passives = TreeRegistry.getInstance().getActivePassives(data.getUnlockedNodes());
        for (PassiveEffect effect : passives) {
            xp = (long) effect.modifyXP(xp);
        }
        // v3 全面增强：应用累加的 xpGainMult stat
        AscensionStatBlock stats = data.buildTotalStats();
        xp = (long) (xp * stats.getEffectiveXpMult());

        boolean leveledUp = data.addAscensionXP(xp);

        if (leveledUp) {
            int lv = data.getAscensionLevel();
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_level_up", lv)
                            .withStyle(ChatFormatting.GOLD), true);
            applyPersistentStats(player, data);
        }

        for (PassiveEffect effect : passives) {
            effect.onKill(player, target, isBoss);
        }

        // 职业固有被动：击杀
        if (data.getMageClass() == MageClass.ABYSSWALKER) {
            player.getFoodData().eat(1, 0.1f);
        }

        syncToClient(player, data);
    }

    // ─── 受伤事件 ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Branch A: player is the ATTACKER (v3 全面增强)
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof Player)) {
            handlePlayerAttacks(event, attacker, event.getEntity());
        }

        // Branch B: player is the TARGET (original logic)
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerAscensionData data = AscensionCapability.get(player);

        // v3 全面增强：dodge chance — totally negate damage
        AscensionStatBlock dodgeStats = data.buildTotalStats();
        float dodgeRoll = dodgeStats.getEffectiveDodgeChance();
        if (dodgeRoll > 0f && player.getRandom().nextFloat() < dodgeRoll) {
            event.setCanceled(true);
            return;
        }

        // 职业固有被动：受伤
        if (data.hasSelectedClass()) {
            switch (data.getMageClass()) {
                case EARTHSHAPER -> event.setAmount(event.getAmount() * 0.97f);
                case STORMCALLER -> {
                    if (event.getSource().getEntity() instanceof LivingEntity atk) {
                        atk.hurt(player.damageSources().magic(), event.getAmount() * 0.01f);
                    }
                }
                default -> {}
            }
            // 通用百分比减伤（来自仪式/精通/天赋）
            AscensionStatBlock stats = data.buildTotalStats();
            float drPct = stats.getEffectiveDamageReductionPercent();
            if (drPct > 0f) {
                event.setAmount(event.getAmount() * (1.0f - drPct));
            }
            // 固定减伤
            if (stats.damageReductionFlat > 0) {
                event.setAmount(Math.max(0, event.getAmount() - stats.damageReductionFlat));
            }
        }

        // 法术减伤（来自飞升树/天赋树节点的 incomingSpellDamageReduction）
        // v3 attribute exposure: read from SPELL_RESIST attribute (authoritative aggregate
        // including equipment/curio/3rd-party modifiers). Cap at 0.95 (attribute max).
        // R72 完全体目标: 专精同元素 75% / 异元素 25% / 全能任意 50%。
        if (event.getSource().getDirectEntity() instanceof com.huige233.transcend.spell.SpellProjectile spProj) {
            float baseResist = Math.min(0.95f,
                    (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.SPELL_RESIST.get()));
            com.huige233.transcend.spell.SpellElement incoming = spProj.getElement();
            ElementMastery mastery = data.getMastery();
            float bonus = 0f;
            if (mastery == ElementMastery.OMNI) {
                bonus = 0.50f;                                                    // 全能对任意元素 50%
            } else if (mastery.isSpecific() && incoming != null) {
                bonus = (mastery.element == incoming) ? 0.75f : 0.25f;            // 专精同元素 75% / 异元素 25%
            }
            float reduction = Math.min(0.95f, baseResist + bonus);
            if (reduction > 0) {
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }

        List<PassiveEffect> passives = TreeRegistry.getInstance().getActivePassives(data.getUnlockedNodes());
        for (PassiveEffect effect : passives) {
            if (event.isCanceled()) break;
            effect.onHurt(event, player, data.getStage());
        }
    }

    /**
     * Handle player → enemy hit logic: dispatch onAttack passives + apply lifesteal heal.
     * v3 新增 — 让 ExecuteThreshold 等需要 attacker-side 触发的被动正常工作。
     */
    private static void handlePlayerAttacks(LivingHurtEvent event, ServerPlayer attacker, LivingEntity target) {
        PlayerAscensionData data = AscensionCapability.get(attacker);
        List<PassiveEffect> passives = TreeRegistry.getInstance().getActivePassives(data.getUnlockedNodes());
        for (PassiveEffect effect : passives) {
            if (event.isCanceled()) break;
            effect.onAttack(event, attacker, target);
        }

        // Lifesteal: heal a fraction of damage dealt
        AscensionStatBlock stats = data.buildTotalStats();
        float lifesteal = stats.getEffectiveLifesteal();
        if (lifesteal > 0f && event.getAmount() > 0f) {
            attacker.heal(event.getAmount() * lifesteal);
        }
    }

    // ─── 玩家重生事件（重新注入属性） ────────────────────────────────

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerAscensionData data = AscensionCapability.get(sp);
            applyPersistentStats(sp, data);
        }
    }

    // ─── 属性注入 ─────────────────────────────────────────────────────────

    /**
     * 将合并后的属性块注入玩家的 Vanilla Attribute 系统。
     * 每次调用先移除旧的修改器再重新添加（幂等）。
     *
     * 注入的属性：
     *  - maxHealth += bonusMaxHealth
     *  - movementSpeed *= (1 + moveSpeedBonus)
     */
    public static void applyPersistentStats(ServerPlayer player, PlayerAscensionData data) {
        AscensionStatBlock stats = data.buildTotalStats();

        // ── 最大生命 ────────────────────────────────────────────────
        AttributeInstance maxHpAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.removeModifier(UUID_HP);
            if (stats.bonusMaxHealth > 0) {
                maxHpAttr.addPermanentModifier(new AttributeModifier(
                        UUID_HP,
                        "transcend_ascension_hp",
                        stats.bonusMaxHealth,
                        AttributeModifier.Operation.ADDITION));
            }
        }

        // ── 移动速度 ────────────────────────────────────────────────
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(UUID_SPEED);
            if (stats.moveSpeedBonus > 0) {
                speedAttr.addPermanentModifier(new AttributeModifier(
                        UUID_SPEED,
                        "transcend_ascension_speed",
                        stats.moveSpeedBonus,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // ── 法术强度（v3 attribute exposure）─────────────────────────
        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.SPELL_POWER.get(),
                UUID_SPELL_POWER, "transcend_ascension_spell_power",
                stats.spellPowerBonus, AttributeModifier.Operation.ADDITION);

        // ── 法术抗性 ─────────────────────────────────────────────────
        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.SPELL_RESIST.get(),
                UUID_SPELL_RESIST, "transcend_ascension_spell_resist",
                stats.incomingSpellDamageReduction, AttributeModifier.Operation.ADDITION);

        // ── 最大魔力 ─────────────────────────────────────────────────
        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.MAX_MANA.get(),
                UUID_MAX_MANA, "transcend_ascension_max_mana",
                stats.bonusManaCapacity, AttributeModifier.Operation.ADDITION);

        // ── 魔力恢复 ─────────────────────────────────────────────────
        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.MANA_REGEN.get(),
                UUID_MANA_REGEN, "transcend_ascension_mana_regen",
                stats.manaRegenBonus, AttributeModifier.Operation.ADDITION);

        // ── 暴击率 ───────────────────────────────────────────────────
        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.CRIT_CHANCE.get(),
                UUID_CRIT_CHANCE, "transcend_ascension_crit_chance",
                stats.critChance, AttributeModifier.Operation.ADDITION);

        // ── 冷却缩减 ─────────────────────────────────────────────────
        applyTranscendAttribute(player,
                com.huige233.transcend.TranscendAttributes.COOLDOWN_REDUCTION.get(),
                UUID_CDR, "transcend_ascension_cdr",
                stats.cooldownReduction, AttributeModifier.Operation.ADDITION);

        // 确保当前HP不超过新上限
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Generic helper: idempotently apply a single AttributeModifier to a target attribute on the player.
     * Removes the existing modifier with this UUID and re-adds it if value > 0.
     */
    private static void applyTranscendAttribute(ServerPlayer player,
                                                net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                                UUID uuid, String name, double value,
                                                AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(uuid);
        if (value > 0) {
            inst.addPermanentModifier(new AttributeModifier(uuid, name, value, op));
        }
    }

    // ─── 仪式完成处理（由物品/命令触发）─────────────────────────────────

    /**
     * 尝试完成指定仪式，成功时广播消息并重新注入属性。
     * @return 是否成功完成
     */
    public static boolean tryCompleteRitual(ServerPlayer player,
                                            PlayerAscensionData data,
                                            AscensionRitual ritual) {
        boolean ok = data.tryCompleteRitual(ritual);
        if (!ok) return false;

        // 广播仪式完成消息
        player.sendSystemMessage(
                Component.translatable("msg.transcend.ritual_complete",
                        ritual.getDisplayName())
                        .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(
                ritual.getRewardText());

        // 重新注入属性
        applyPersistentStats(player, data);
        syncToClient(player, data);

        // 仪式特殊效果
        applyRitualEffects(player, ritual);

        // 升天粒子特效 + 音效（全局广播）
        player.level().broadcastEntityEvent(player, (byte) 35); // Vanilla firework burst

        // v3 ascension integration: hint player to bind a vow for the newly unlocked stage
        int unlockedStage = ritual.stageIndex + 1;
        if (unlockedStage >= 1 && unlockedStage <= 4 && !data.hasVowForStage(unlockedStage)) {
            player.sendSystemMessage(Component.translatable("msg.transcend.vow_hint", unlockedStage)
                    .withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    // ─── 施法计数（由 TranscendWand 调用）────────────────────────────────

    /**
     * 记录一次施法，由 TranscendWand.castSpell 末尾调用
     */
    public static void recordCast(Player player) {
        if (player.level().isClientSide) return;
        AscensionCapability.ifPresent(player, data -> {
            data.addCast();
            // 达到施法里程碑时提示
            long casts = data.getTotalCasts();
            if (casts == 100 || casts == 500 || casts == 1000 || casts == 5000) {
                if (player instanceof ServerPlayer sp) syncToClient(sp, data);
            }
        });
    }

    // ─── 网络同步 ─────────────────────────────────────────────────────────

    public static void syncToClient(ServerPlayer player, PlayerAscensionData data) {
        com.huige233.transcend.handle.NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.huige233.transcend.network.S2CAscensionSync(data));
    }

    /**
     * Check if any active mana_free_cast passive grants a free cast this time.
     * Iterates all active passives — first one to roll true wins.
     *
     * <p>Cast sites (e.g. TranscendWand) should call this before consuming mana;
     * if it returns true, skip the mana deduction.
     */
    public static boolean tryFreeCast(ServerPlayer player) {
        if (player == null) return false;
        PlayerAscensionData data = AscensionCapability.get(player);
        List<PassiveEffect> passives = TreeRegistry.getInstance().getActivePassives(data.getUnlockedNodes());
        for (PassiveEffect effect : passives) {
            if (effect.rollFreeCast(player)) return true;
        }
        return false;
    }

    /**
     * Attribute-aware crit chance read — includes equipment / Curio / 3rd-party modifiers.
     * Cap matches the RangedAttribute max (0.80).
     */
    public static float getCritChance(net.minecraft.world.entity.player.Player player) {
        if (player == null) return 0f;
        return Math.min(0.80f,
                (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.CRIT_CHANCE.get()));
    }

    /**
     * Attribute-aware cooldown reduction read — includes equipment / Curio / 3rd-party modifiers.
     * Cap matches the RangedAttribute max (0.75).
     */
    public static float getCDR(net.minecraft.world.entity.player.Player player) {
        if (player == null) return 0f;
        return Math.min(0.75f,
                (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.COOLDOWN_REDUCTION.get()));
    }

    // ─── 玩家 tick：魔力恢复 ───────────────────────────────────────────────
    private static final String REGEN_ACCUM_TAG = "transcend_mana_regen_accum";

    /**
     * 计算环境魔力倍率 — 影响 MANA_REGEN 实际生效值。
     *
     * <p>v4: 动态环境系统。多因素叠加，最终倍率范围约 [0.4, 2.8]。
     *
     * <ul>
     *   <li>Y 层 (深层魔力丰富)：Y &lt; 0 +60%, 0..40 +30%, 40..100 基准, &gt;100 高空 +20% (天空魔力)</li>
     *   <li>露天 (canSeeSky)：白天 +20%, 夜晚 +10% (月光也算)</li>
     *   <li>下界/末地：下界 -30% (混乱), 末地 +40% (虚空灵能)</li>
     *   <li>天气：下雨 -20% (空气湿润抑制法波), 雷雨 +30% (能量充沛)</li>
     *   <li>生物群系：Mystic / Magic 类 +50%, 沙漠/恶地 -30%, 海洋 +20%, 森林 +10%</li>
     * </ul>
     */
    public static double computeEnvironmentManaMultiplier(ServerPlayer sp) {
        double mult = 1.0;
        net.minecraft.world.level.Level level = sp.level();
        net.minecraft.core.BlockPos pos = sp.blockPosition();

        // ── Y 层 ─────────────────────────────────────────────────────────
        int y = pos.getY();
        if (y < 0)        mult *= 1.6;
        else if (y < 40)  mult *= 1.3;
        else if (y > 100) mult *= 1.2;

        // ── 露天 / 天空魔力 ──────────────────────────────────────────────
        boolean canSeeSky = level.canSeeSky(pos);
        if (canSeeSky) {
            boolean day = level.isDay();
            mult *= day ? 1.2 : 1.1;
        }

        // ── 维度 ─────────────────────────────────────────────────────────
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = level.dimension();
        if (dim == net.minecraft.world.level.Level.NETHER) mult *= 0.7;
        else if (dim == net.minecraft.world.level.Level.END) mult *= 1.4;

        // ── 天气 ─────────────────────────────────────────────────────────
        if (level.isThundering())     mult *= 1.3;
        else if (level.isRaining())   mult *= 0.8;

        // ── 生物群系 (简化分类) ──────────────────────────────────────────
        var biomeHolder = level.getBiome(pos);
        String biomeName = biomeHolder.unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("");
        if (biomeName.contains("mushroom") || biomeName.contains("ancient")
                || biomeName.contains("deep_dark") || biomeName.contains("lush")) {
            mult *= 1.5;
        } else if (biomeName.contains("ocean") || biomeName.contains("river")
                || biomeName.contains("swamp")) {
            mult *= 1.2;
        } else if (biomeName.contains("forest") || biomeName.contains("jungle")
                || biomeName.contains("taiga")) {
            mult *= 1.1;
        } else if (biomeName.contains("desert") || biomeName.contains("badlands")
                || biomeName.contains("savanna")) {
            mult *= 0.7;
        }

        return Math.max(0.3, Math.min(mult, 3.0));
    }

    /** 服务端跟踪每玩家上次推送的 innate mana 值，仅在变化时发包。 */
    private static final java.util.Map<UUID, Integer> LAST_SYNCED_INNATE = new java.util.concurrent.ConcurrentHashMap<>();
    /** 强制同步周期（防止初次登录或值长时间不变时客户端 0 显示）。 */
    private static final long FORCE_SYNC_INTERVAL_TICKS = 40L;
    private static final java.util.Map<UUID, Long> LAST_SYNC_TICK = new java.util.concurrent.ConcurrentHashMap<>();

    /** 当前 1 秒窗口内累计的实际环境吸收 mana（按 tick 累加，每 20 tick 翻入"上一秒"快照） */
    private static final java.util.Map<UUID, Float> ABSORB_ACCUM = new java.util.concurrent.ConcurrentHashMap<>();
    /** 上一个完整 1 秒窗口的实际吸收量（HUD 显示用），单位 mana/s */
    private static final java.util.Map<UUID, Float> ABSORB_LAST_SEC = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 每 tick 调用：
     *  - 自体恢复：按 MANA_REGEN attribute（每秒，含飞升 manaRegenBonus）/ 20 累积分数 mana
     *    注入玩家内禀池。基础 0.1/s，飞升满层叠加约 1.0/s。<b>不再乘环境倍率</b>。
     *  - 环境吸收：飞升阶段 + 等级越高，每秒额外从所在区块 ChunkManaSavedData 抽取
     *    最多 8 mana 注入内禀池。区块魔力被抽干则吸收为 0。
     *
     * <p>分数 mana 累积在玩家持久 NBT 的 transcend_mana_regen_accum 中，
     * 累积满 1 整数 mana 时才注入 innate 池 — 避免每 tick 浪费整数舍入。
     *
     * <p>每次值发生变化或经过 {@link #FORCE_SYNC_INTERVAL_TICKS} ticks 后强制
     * 推送 {@link com.huige233.transcend.network.S2CInnateManaSync} 给客户端，
     * 让 HUD 显示真实数值（NBT 字段不会自动同步）。
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        UUID uuid = sp.getUUID();
        long now = sp.level().getGameTime();

        // ── 1. 自体恢复：纯 MANA_REGEN 属性，无环境倍率 ────────────────────
        double regenPerSec = sp.getAttributeValue(com.huige233.transcend.TranscendAttributes.MANA_REGEN.get());
        double accumDelta = (regenPerSec > 0) ? regenPerSec / 20.0 : 0.0;

        // ── 2. 环境吸收：满飞升下最多 8/s，从所在区块抽 ChunkManaSavedData ──
        // 公式：absorbPerSec = stage + ascLevel * 0.4
        //   stage 0 + lv 0  -> 0/s
        //   stage 2 + lv 5  -> 4/s
        //   stage 4 + lv 10 -> 8/s (上限)
        int stage = data.getStage();
        int ascLv = data.getAscensionLevel();
        double absorbPerSec = Math.min(8.0, stage + ascLv * 0.4);
        float actualAbsorbed = 0f;
        if (absorbPerSec > 0) {
            double absorbPerTick = absorbPerSec / 20.0;
            // 从所在区块抽取，量被区块剩余魔力截断
            com.huige233.transcend.world.mana.ChunkManaSavedData chunkData =
                    com.huige233.transcend.world.mana.ChunkManaSavedData.get((net.minecraft.server.level.ServerLevel) sp.level());
            actualAbsorbed = chunkData.consumeMana(sp.chunkPosition(), (float) absorbPerTick);
            accumDelta += actualAbsorbed;
        }
        // 累计当前 1 秒窗口内的实际吸收 mana
        ABSORB_ACCUM.merge(uuid, actualAbsorbed, Float::sum);
        // 每 20 tick (= 1 秒) 把累计值翻到 LAST_SEC 快照，HUD 据此显示
        if (now % 20 == 0) {
            ABSORB_LAST_SEC.put(uuid, ABSORB_ACCUM.getOrDefault(uuid, 0f));
            ABSORB_ACCUM.put(uuid, 0f);
        }

        if (accumDelta > 0) {
            net.minecraft.nbt.CompoundTag pdata = sp.getPersistentData();
            double accum = pdata.getDouble(REGEN_ACCUM_TAG) + accumDelta;
            int wholeManas = (int) accum;
            pdata.putDouble(REGEN_ACCUM_TAG, accum - wholeManas);

            if (wholeManas > 0) {
                int currentInnate = com.huige233.transcend.client.magic.MagicCrystalHelper.getInnateMana(sp);
                int maxInnate = com.huige233.transcend.client.magic.MagicCrystalHelper.getInnateMaxMana(sp);
                if (currentInnate < maxInnate) {
                    com.huige233.transcend.client.magic.MagicCrystalHelper.setInnateMana(sp,
                            currentInnate + wholeManas);
                }
            }
        }

        // ── 3. 同步 innate mana + 吸收速率 给客户端 ───────────────────────
        int innateNow = com.huige233.transcend.client.magic.MagicCrystalHelper.getInnateMana(sp);
        float absorbHud = ABSORB_LAST_SEC.getOrDefault(uuid, 0f);
        Integer lastSent = LAST_SYNCED_INNATE.get(uuid);
        long lastSync = LAST_SYNC_TICK.getOrDefault(uuid, -FORCE_SYNC_INTERVAL_TICKS);
        boolean valueChanged = (lastSent == null || lastSent != innateNow);
        boolean forcedRefresh = (now - lastSync) >= FORCE_SYNC_INTERVAL_TICKS;
        if (valueChanged || forcedRefresh) {
            com.huige233.transcend.handle.NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new com.huige233.transcend.network.S2CInnateManaSync(innateNow, absorbHud));
            LAST_SYNCED_INNATE.put(uuid, innateNow);
            LAST_SYNC_TICK.put(uuid, now);
        }
    }

    // ─── 誓约效果应用 ─────────────────────────────────────────────────────

    /**
     * 应用誓约效果到玩家属性。
     * 在飞升数据同步或变更时调用。
     *
     * 当前为框架方法 — 实际属性修改需要与现有的合并属性管线
     * （buildTotalStats / applyPersistentStats）集成。这里仅遍历四个阶段，
     * 查表得到 AscensionVow，并预留 TODO 用于将誓约提供的 spellDamage、
     * circleLimitAdd 等数值注入到玩家最终属性块中。
     */
    public static void applyVowEffects(ServerPlayer player, PlayerAscensionData data) {
        if (player == null || data == null) return;

        for (int stage = 1; stage <= 4; stage++) {
            String vowId = data.getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;

            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;

            // TODO: 将 vow.getSpellDamageBonus()、vow.getCircleLimitAdd()、
            // vow.getManaCostMult()、vow.getCdrAdd() 等数值并入玩家的合并属性块。
            // 当前 buildTotalStats() 不感知誓约 — 后续需在该处增加 vow 来源叠加，
            // 或在此处通过 AttributeModifier 注入新的 UUID 修改器。
        }
    }

    // ─── 仪式特殊效果 ─────────────────────────────────────────────────────

    private static void applyRitualEffects(ServerPlayer player, AscensionRitual ritual) {
        switch (ritual) {
            case AWAKENING -> {
                player.heal(player.getMaxHealth());
                player.getFoodData().eat(20, 20f);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 200, 0));
            }
            case TEMPERING -> {
                player.heal(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600, 1));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 400, 2));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 600, 1));
            }
            case PURIFICATION -> {
                player.heal(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 2));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 600, 3));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1200, 0));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1200, 0));
                for (net.minecraft.world.effect.MobEffectInstance effect : new java.util.ArrayList<>(player.getActiveEffects())) {
                    if (!effect.getEffect().isBeneficial()) player.removeEffect(effect.getEffect());
                }
            }
            case TRANSCENDENCE -> {
                player.heal(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2400, 3));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 1200, 4));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 2400, 4));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE, 6000, 0));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 2400, 2));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 2400, 1));
                for (net.minecraft.world.effect.MobEffectInstance effect : new java.util.ArrayList<>(player.getActiveEffects())) {
                    if (!effect.getEffect().isBeneficial()) player.removeEffect(effect.getEffect());
                }
                player.level().broadcastEntityEvent(player, (byte) 35);
            }
        }
    }

    // ─── R74 + R75: 完全体追加属性 event 钩子 ───────────────────────────

    /**
     * R74 + R75 修正: 治疗效果增强 + 自然恢复增强 — 真正 hook vanilla regen 路径。
     * <ul>
     *   <li>{@code healingReceivedBonus} → 所有治疗 ×(1 + bonus)</li>
     *   <li>{@code naturalRegenBonus} → 仅当食物 ≥ 18 (vanilla regen 触发条件) 且 heal 量看起来像 vanilla regen 时 ×(1 + bonus)</li>
     * </ul>
     * Vanilla 自然恢复每 80 tick 触发一次 heal(1.0)，或饱满状态 heal(saturation/6) per 10 tick — 都是小数值。
     * 区分逻辑：heal 量 ≤ 2.0 + food ≥ 18 → 视为 vanilla regen。
     */
    @SubscribeEvent
    public static void onLivingHealAscension(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();

        float mult = 1.0f + stats.healingReceivedBonus;

        // 自然恢复加成：vanilla 食物 regen 触发条件 + 小幅 heal 量 → 视为自然恢复
        if (stats.naturalRegenBonus > 0
                && event.getAmount() > 0
                && event.getAmount() <= 2.0f
                && player.getFoodData().getFoodLevel() >= 18) {
            mult *= (1.0f + stats.naturalRegenBonus);
        }

        if (mult != 1.0f) {
            event.setAmount(event.getAmount() * mult);
        }
    }

    /**
     * R74: 死亡保命 — stage 4 + level 10 时启用。致命伤减为留 1HP，冷却 5 分钟。
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onDeathSave(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.deathSaveEnabled <= 0) return;
        if (data.getStage() < 4 || data.getAscensionLevel() < 10) return;

        long now = player.level().getGameTime();
        long cooldownTicks = 6000L;  // 5 min × 60s × 20t/s
        if (now - data.getLastDeathSaveAt() < cooldownTicks) return;

        if (player.getHealth() - event.getAmount() > 0) return;

        float capped = Math.max(0f, player.getHealth() - 1.0f);
        event.setAmount(capped);
        data.setLastDeathSaveAt(now);
        com.huige233.transcend.ascension.AscensionHandler.syncToClient(player, data);

        player.displayClientMessage(
                Component.translatable("msg.transcend.death_save.triggered")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.TOTEM_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.5, 0.5, 0.5);
        }
    }

    /**
     * R74 + R75: 饱食度消耗减免 — 真正 hook vanilla exhaustion 累加。
     * <p>vanilla {@code FoodData.exhaustionLevel} 每 tick 由 sprint/jump/attack/heal 等动作累加。
     * 每 tick 计算 delta，若为正（被动作消耗），扣除 {@code foodConsumptionReduction} 比例（即 refund）。
     * 实现：用反射读 / 写 {@code exhaustionLevel} 私有字段。
     */
    @SubscribeEvent
    public static void onPlayerTickR74Food(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.foodConsumptionReduction <= 0) return;

        try {
            net.minecraft.world.food.FoodData fd = player.getFoodData();
            java.lang.reflect.Field f = net.minecraft.world.food.FoodData.class.getDeclaredField("exhaustionLevel");
            f.setAccessible(true);
            float currExh = f.getFloat(fd);
            Float prevExh = EXHAUSTION_TRACKING.get(player.getUUID());

            if (prevExh != null && currExh > prevExh) {
                float delta = currExh - prevExh;
                float refund = delta * stats.foodConsumptionReduction;
                float newExh = Math.max(0f, currExh - refund);
                f.setFloat(fd, newExh);
                EXHAUSTION_TRACKING.put(player.getUUID(), newExh);
            } else {
                EXHAUSTION_TRACKING.put(player.getUUID(), currExh);
            }
        } catch (Exception ignored) {
            // 反射失败：跳过当前 tick；下次再试
        }
    }

    /** R75: 玩家 UUID → 上一 tick 的 exhaustionLevel（用于 delta 计算） */
    private static final java.util.Map<java.util.UUID, Float> EXHAUSTION_TRACKING = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * R75: 摔落伤害减免 — hook {@link net.minecraftforge.event.entity.living.LivingFallEvent}
     * 让 vanilla fall damage 计算直接被乘 (1 - reduction)。
     */
    @SubscribeEvent
    public static void onLivingFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.fallDamageReduction <= 0) return;

        event.setDamageMultiplier(event.getDamageMultiplier() * (1.0f - stats.fallDamageReduction));
    }

    /**
     * R75: 控制抗性 — vanilla 负面 MobEffect 持续时间缩短 (1 - resistance)。
     * <p>用反射改 {@link net.minecraft.world.effect.MobEffectInstance#duration} 私有字段。
     * 仅对 {@link net.minecraft.world.effect.MobEffectCategory#HARMFUL} 生效。
     */
    @SubscribeEvent
    public static void onMobEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        net.minecraft.world.effect.MobEffectInstance inst = event.getEffectInstance();
        if (inst.getEffect().getCategory() != net.minecraft.world.effect.MobEffectCategory.HARMFUL) return;

        PlayerAscensionData data = AscensionCapability.get(player);
        AscensionStatBlock stats = data.buildTotalStats();
        if (stats.controlResistance <= 0) return;

        int oldDuration = inst.getDuration();
        int newDuration = (int) (oldDuration * (1.0f - stats.controlResistance));
        if (newDuration < 20) newDuration = 20; // 最低 1 秒
        if (newDuration >= oldDuration) return;

        try {
            java.lang.reflect.Field f = net.minecraft.world.effect.MobEffectInstance.class.getDeclaredField("duration");
            f.setAccessible(true);
            f.setInt(inst, newDuration);
        } catch (Exception ignored) {
            // 反射失败：保持原值
        }
    }

    // ─── R76: 灵魂烙印（Soul Mark）─────────────────────────────────────

    /** R76: 灵魂烙印复活点 — 死亡后传送到最近的同维度烙印锚（验证锚仍存在且属于该玩家）。 */
    @SubscribeEvent
    public static void onSoulMarkRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        if (data.getSoulMarks().isEmpty()) return;

        net.minecraft.resources.ResourceLocation dimKey = sp.level().dimension().location();
        var nearest = data.findNearestSoulMark(dimKey, sp.blockPosition());
        if (nearest == null) return;

        net.minecraft.core.BlockPos teleportPos = nearest.pos();
        net.minecraft.world.level.block.entity.BlockEntity be = sp.level().getBlockEntity(teleportPos);

        // 锚已不存在 / 类型不对 → 清理无效烙印
        if (!(be instanceof com.huige233.transcend.block.ascension.AscensionAnchorBlockEntity anchorBe)) {
            data.removeSoulMark(dimKey, teleportPos);
            syncToClient(sp, data);
            return;
        }
        // 锚被别人占用了 → 清理无效烙印
        if (!sp.getUUID().equals(anchorBe.getSoulMarkOwner())) {
            data.removeSoulMark(dimKey, teleportPos);
            syncToClient(sp, data);
            return;
        }

        sp.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY() + 1.0, teleportPos.getZ() + 0.5);
        sp.displayClientMessage(
                Component.translatable("msg.transcend.soul_mark.respawned")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sp.level().playSound(null, teleportPos,
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
        if (sp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 1.5, teleportPos.getZ() + 0.5,
                    40, 0.5, 1.0, 0.5, 0.3);
        }
    }

    /**
     * R76: 灵魂烙印范围 buff — 每 3 秒检查；若处于最近烙印锚 100 格球内，
     * 给予 Regeneration I (4s) + Resistance I (4s)。
     *
     * <p>使用 isAmbient=true / showIcon=true（HUD 显示图标，但无环绕粒子）。
     */
    @SubscribeEvent
    public static void onSoulMarkRangeBuff(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 60 != 0) return;

        PlayerAscensionData data = AscensionCapability.get(sp);
        if (data.getSoulMarks().isEmpty()) return;

        net.minecraft.resources.ResourceLocation dimKey = sp.level().dimension().location();
        var nearest = data.findNearestSoulMark(dimKey, sp.blockPosition());
        if (nearest == null) return;

        // 锚仍属于本人 + 100 格内 → 给予 buff
        net.minecraft.world.level.block.entity.BlockEntity be = sp.level().getBlockEntity(nearest.pos());
        if (!(be instanceof com.huige233.transcend.block.ascension.AscensionAnchorBlockEntity anchorBe)) return;
        if (!sp.getUUID().equals(anchorBe.getSoulMarkOwner())) return;

        double distSq = nearest.pos().distSqr(sp.blockPosition());
        if (distSq > 100.0 * 100.0) return;

        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 80, 0, true, false, true));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
    }
}
