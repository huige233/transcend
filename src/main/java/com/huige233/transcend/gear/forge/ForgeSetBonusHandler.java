package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * R92: 造物之道套装协同 — 玩家穿戴的 forged 装备组合自动激活的被动效果。
 *
 * <p>玩家请求："套装协同 作为效果 而不是大招" — 全被动，无主动操作，无按键。
 *
 * <h2>3 维度套装检测</h2>
 * <ul>
 *   <li><b>Aspect 套装</b>：4+ 件 forged 装备的 crucible aspect 同 dominant 元素 → 激活 6 个 aspect 套装之一</li>
 *   <li><b>共鸣套装</b>：全身 socket 总数中某一类 ≥ 8 → 激活 6 个 socket 套装之一</li>
 *   <li><b>加冕套装</b>：武器 + 任 1 件护甲的 blessing 同 dominant 元素 → 激活 4 个 blessing 套装之一</li>
 * </ul>
 *
 * <h2>实现机制</h2>
 * <ul>
 *   <li><b>AttributeModifier (Transient)</b>：数值类 buff（攻击/护甲/移速/最大生命/韧性）— 每 20 tick 检查并 add/remove</li>
 *   <li><b>MobEffect (40 tick, 反复刷新)</b>：状态类 buff（火免疫/水下呼吸/夜视/隐身）— 不显示 icon，不闪烁</li>
 * </ul>
 *
 * <h2>性能</h2>
 * <p>仅 ServerPlayer + 每 20 tick 扫描 1 次（== 1 秒）+ 增量 add/remove modifier（仅在 activate 状态变化时）。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeSetBonusHandler {

    private static final int CHECK_INTERVAL = 20;        // 1 秒检查一次
    private static final int EFFECT_DURATION = 40;       // 2 秒 MobEffect duration（每秒刷新，永不掉）
    private static final int ASPECT_THRESHOLD = 4;       // 4 件同 dominant 激活 aspect 套装
    private static final int SOCKET_THRESHOLD = 8;       // 8 个同种 socket 激活共鸣套装

    // ── 稳定 UUID（per-bonus）─────────────────────────────────────────
    // Aspect 套装
    private static final UUID UUID_ASPECT_EARTH_ARMOR  = UUID.fromString("b1000000-0001-0001-0001-000000000001");
    private static final UUID UUID_ASPECT_WIND_SPEED   = UUID.fromString("b1000000-0002-0002-0002-000000000002");
    // 共鸣套装
    private static final UUID UUID_SOCKET_SHARP_DMG    = UUID.fromString("b2000000-0001-0001-0001-000000000001");
    private static final UUID UUID_SOCKET_WARD_TOUGH   = UUID.fromString("b2000000-0002-0002-0002-000000000002");
    private static final UUID UUID_SOCKET_LEECH_KB     = UUID.fromString("b2000000-0003-0003-0003-000000000003");
    private static final UUID UUID_SOCKET_SPARK_LUCK   = UUID.fromString("b2000000-0004-0004-0004-000000000004");
    private static final UUID UUID_SOCKET_SWIFT_SPEED  = UUID.fromString("b2000000-0005-0005-0005-000000000005");
    private static final UUID UUID_SOCKET_FOCUS_HP     = UUID.fromString("b2000000-0006-0006-0006-000000000006");
    // 加冕套装
    private static final UUID UUID_BLESSING_STAR_LUCK  = UUID.fromString("b3000000-0003-0003-0003-000000000003");
    private static final UUID UUID_BLESSING_SUN_ATTACK = UUID.fromString("b3000000-0001-0001-0001-000000000001");
    private static final UUID UUID_BLESSING_MOON_ATTACK = UUID.fromString("b3000000-0002-0002-0002-000000000002");

    // ── 套装数值 ─────────────────────────────────────────────────────
    private static final double EARTH_ARMOR        = 2.0;   // ADDITION
    private static final double WIND_SPEED         = 0.05;  // MULTIPLY_BASE (+5%)
    private static final double SHARP_SET_DMG      = 1.0;   // ADDITION
    private static final double WARD_SET_TOUGH     = 2.0;   // ADDITION
    private static final double LEECH_SET_KB       = 1.0;   // ADDITION (KNOCKBACK_RESISTANCE)
    private static final double SPARK_SET_LUCK     = 1.0;   // ADDITION (Luck)
    private static final double SWIFT_SET_SPEED    = 0.05;  // MULTIPLY_BASE
    private static final double FOCUS_SET_HP       = 4.0;   // ADDITION (+2 颗心)
    private static final double STAR_LUCK          = 1.0;   // ADDITION
    private static final double SUN_DAY_ATTACK     = 1.0;   // ADDITION (仅白昼)
    private static final double MOON_NIGHT_ATTACK  = 1.0;   // ADDITION (仅夜晚)

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % CHECK_INTERVAL != 0) return;

        // ── Step 1: 扫描 5 槽（MAINHAND + 4 ARMOR）─────────────────────
        Map<AspectKind, Integer> aspectCounts = new EnumMap<>(AspectKind.class);
        Map<ResonanceKind, Integer> socketCounts = new EnumMap<>(ResonanceKind.class);
        Map<CelestialKind, Integer> blessingCounts = new EnumMap<>(CelestialKind.class);

        ItemStack[] slots = collectSlots(player);
        for (ItemStack item : slots) {
            if (item.isEmpty() || !GearForgeData.isInPipeline(item)) continue;

            // Aspect dominant
            var crucible = GearForgeData.getCrucible(item);
            if (crucible != null) {
                AspectDef def = AspectRegistry.byId(crucible.aspect());
                if (def != null && def != AspectRegistry.INDETERMINATE) {
                    aspectCounts.merge(def.dominant(), 1, Integer::sum);
                }
            }

            // Sockets
            for (var sock : GearForgeData.getSockets(item)) {
                ResonanceKind k = ResonanceKind.byId(sock.crystalId());
                if (k != null) socketCounts.merge(k, 1, Integer::sum);
            }

            // Blessing dominant
            var blessing = GearForgeData.getCelestial(item);
            if (blessing != null) {
                BlessingDef bd = BlessingRegistry.byId(blessing.blessing());
                if (bd != null && bd != BlessingRegistry.INDETERMINATE) {
                    blessingCounts.merge(bd.dominant(), 1, Integer::sum);
                }
            }
        }

        // ── Step 2: 应用 Aspect 套装 ──────────────────────────────────
        applyAspectBonus(player, aspectCounts);

        // ── Step 3: 应用 共鸣 套装 ────────────────────────────────────
        applySocketBonus(player, socketCounts);

        // ── Step 4: 应用 加冕 套装 ────────────────────────────────────
        applyBlessingBonus(player, blessingCounts);
    }

    private static ItemStack[] collectSlots(Player player) {
        return new ItemStack[]{
                player.getItemBySlot(EquipmentSlot.MAINHAND),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET),
        };
    }

    // ─── Aspect 套装 ───────────────────────────────────────────────────

    private static void applyAspectBonus(Player p, Map<AspectKind, Integer> counts) {
        boolean fire   = counts.getOrDefault(AspectKind.FIRE,   0) >= ASPECT_THRESHOLD;
        boolean water  = counts.getOrDefault(AspectKind.WATER,  0) >= ASPECT_THRESHOLD;
        boolean earth  = counts.getOrDefault(AspectKind.EARTH,  0) >= ASPECT_THRESHOLD;
        boolean wind   = counts.getOrDefault(AspectKind.WIND,   0) >= ASPECT_THRESHOLD;
        boolean spirit = counts.getOrDefault(AspectKind.SPIRIT, 0) >= ASPECT_THRESHOLD;
        boolean voidk  = counts.getOrDefault(AspectKind.VOID,   0) >= ASPECT_THRESHOLD;

        // FIRE: 火焰免疫
        if (fire) p.addEffect(makeAmbient(MobEffects.FIRE_RESISTANCE));

        // WATER: 水下呼吸 + 海豚之力
        if (water) {
            p.addEffect(makeAmbient(MobEffects.WATER_BREATHING));
            p.addEffect(makeAmbient(MobEffects.DOLPHINS_GRACE));
        }

        // EARTH: +2 ARMOR
        manageModifier(p, Attributes.ARMOR, UUID_ASPECT_EARTH_ARMOR,
                "transcend.set.aspect.earth", earth, EARTH_ARMOR,
                AttributeModifier.Operation.ADDITION);

        // WIND: +5% MOVEMENT_SPEED + 跳跃 buff（JUMP_BOOST I 周期，给玩家 vertical 加成）
        manageModifier(p, Attributes.MOVEMENT_SPEED, UUID_ASPECT_WIND_SPEED,
                "transcend.set.aspect.wind.speed", wind, WIND_SPEED,
                AttributeModifier.Operation.MULTIPLY_BASE);
        if (wind) p.addEffect(makeAmbient(MobEffects.JUMP));

        // SPIRIT: 夜视
        if (spirit) p.addEffect(makeAmbient(MobEffects.NIGHT_VISION));

        // VOID: 潜行时隐身
        if (voidk && p.isShiftKeyDown()) {
            p.addEffect(makeAmbient(MobEffects.INVISIBILITY));
        }
    }

    // ─── 共鸣 套装 ─────────────────────────────────────────────────────

    private static void applySocketBonus(Player p, Map<ResonanceKind, Integer> counts) {
        boolean sharpSet  = counts.getOrDefault(ResonanceKind.SHARPNESS, 0) >= SOCKET_THRESHOLD;
        boolean wardSet   = counts.getOrDefault(ResonanceKind.WARD,      0) >= SOCKET_THRESHOLD;
        boolean leechSet  = counts.getOrDefault(ResonanceKind.LEECH,     0) >= SOCKET_THRESHOLD;
        boolean sparkSet  = counts.getOrDefault(ResonanceKind.SPARK,     0) >= SOCKET_THRESHOLD;
        boolean swiftSet  = counts.getOrDefault(ResonanceKind.SWIFTNESS, 0) >= SOCKET_THRESHOLD;
        boolean focusSet  = counts.getOrDefault(ResonanceKind.FOCUS,     0) >= SOCKET_THRESHOLD;

        manageModifier(p, Attributes.ATTACK_DAMAGE, UUID_SOCKET_SHARP_DMG,
                "transcend.set.socket.sharpness", sharpSet, SHARP_SET_DMG,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.ARMOR_TOUGHNESS, UUID_SOCKET_WARD_TOUGH,
                "transcend.set.socket.ward", wardSet, WARD_SET_TOUGH,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.KNOCKBACK_RESISTANCE, UUID_SOCKET_LEECH_KB,
                "transcend.set.socket.leech", leechSet, LEECH_SET_KB,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.LUCK, UUID_SOCKET_SPARK_LUCK,
                "transcend.set.socket.spark", sparkSet, SPARK_SET_LUCK,
                AttributeModifier.Operation.ADDITION);

        manageModifier(p, Attributes.MOVEMENT_SPEED, UUID_SOCKET_SWIFT_SPEED,
                "transcend.set.socket.swiftness", swiftSet, SWIFT_SET_SPEED,
                AttributeModifier.Operation.MULTIPLY_BASE);

        manageModifier(p, Attributes.MAX_HEALTH, UUID_SOCKET_FOCUS_HP,
                "transcend.set.socket.focus", focusSet, FOCUS_SET_HP,
                AttributeModifier.Operation.ADDITION);
    }

    // ─── 加冕 套装（武器 + 任 1 件护甲同 dominant 元素）────────────────

    private static void applyBlessingBonus(Player p, Map<CelestialKind, Integer> counts) {
        // 至少 2 件同 dominant（武器 + ≥1 护甲）
        boolean sunSet   = counts.getOrDefault(CelestialKind.SUN,   0) >= 2;
        boolean moonSet  = counts.getOrDefault(CelestialKind.MOON,  0) >= 2;
        boolean starSet  = counts.getOrDefault(CelestialKind.STAR,  0) >= 2;
        boolean abyssSet = counts.getOrDefault(CelestialKind.ABYSS, 0) >= 2;

        // SUN 套装：白昼 +1.0 ATTACK_DAMAGE
        boolean sunActive = sunSet && p.level().isDay();
        manageModifier(p, Attributes.ATTACK_DAMAGE, UUID_BLESSING_SUN_ATTACK,
                "transcend.set.blessing.sun", sunActive, SUN_DAY_ATTACK,
                AttributeModifier.Operation.ADDITION);

        // MOON 套装：夜晚 +1.0 ATTACK_DAMAGE
        boolean moonActive = moonSet && p.level().isNight();
        manageModifier(p, Attributes.ATTACK_DAMAGE, UUID_BLESSING_MOON_ATTACK,
                "transcend.set.blessing.moon", moonActive, MOON_NIGHT_ATTACK,
                AttributeModifier.Operation.ADDITION);

        // STAR 套装：永久 +1 LUCK
        manageModifier(p, Attributes.LUCK, UUID_BLESSING_STAR_LUCK,
                "transcend.set.blessing.star", starSet, STAR_LUCK,
                AttributeModifier.Operation.ADDITION);

        // ABYSS 套装：水中时 water breathing
        if (abyssSet && p.isInWater()) {
            p.addEffect(makeAmbient(MobEffects.WATER_BREATHING));
        }
    }

    // ─── 工具方法 ─────────────────────────────────────────────────────

    /** MobEffect 周期施加 — duration 2 秒，每秒由 PlayerTick 刷新，玩家不会看到 buff icon spam。 */
    private static MobEffectInstance makeAmbient(net.minecraft.world.effect.MobEffect effect) {
        // amplifier 0, ambient=true（弱化 HUD 显示）, visible=false（不显示 icon）, showIcon=false
        return new MobEffectInstance(effect, EFFECT_DURATION, 0, true, false, false);
    }

    /**
     * 增量 modifier 管理 — 仅在 activate 状态变化时 remove/add，避免每 tick 闪烁。
     *
     * @param attr      目标 attribute
     * @param uuid      稳定 UUID（per-modifier）
     * @param name      modifier name（用于序列化标识，不本地化）
     * @param activate  是否激活套装
     * @param amount    数值
     * @param op        ADDITION / MULTIPLY_BASE / MULTIPLY_TOTAL
     */
    private static void manageModifier(Player p, Attribute attr, UUID uuid, String name,
                                        boolean activate, double amount,
                                        AttributeModifier.Operation op) {
        if (attr == null) return;
        AttributeInstance instance = p.getAttribute(attr);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (activate) {
            if (existing == null) {
                instance.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
            }
            // existing != null 且 amount 一致 → 无操作（避免无谓 remove/add）
        } else {
            if (existing != null) instance.removeModifier(uuid);
        }
    }
}
