package com.huige233.transcend.gear;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.forge.AspectDef;
import com.huige233.transcend.gear.forge.AspectRegistry;
import com.huige233.transcend.gear.forge.BlessingDef;
import com.huige233.transcend.gear.forge.BlessingRegistry;
import com.huige233.transcend.gear.forge.ForgeBattleConfig;
import com.huige233.transcend.gear.forge.ResonanceKind;
import com.huige233.transcend.gear.forge.TriggerAffixKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R80: 造物之道通用 tooltip 注入器。
 *
 * <p>检测 ItemStack 是否带有 {@link GearForgeData#ROOT_TAG} 子标签 OR 是 {@link GearForgeData#isEligibleForPipeline 合格} 装备。
 * 若是，注入 7 行进度概览：
 * <pre>
 * ⚒ 造物之道 (3/5)
 *   ✦ 坩埚: 炽红 (+10% 伤害)
 *   ◇ 共鸣: 2/4 词槽
 *   ◇ 灵魂: 1/3 烙印
 *   ○ 经历: 未觉醒
 *   ○ 天命: 未加冕
 * </pre>
 *
 * <p>已写入阶段：紫色/对应色 + ✦/◇ marker。
 * <br>未写入阶段：灰色斜体 + ○ marker。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GearForgeTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        boolean inPipeline = GearForgeData.isInPipeline(stack);
        boolean eligible   = GearForgeData.isEligibleForPipeline(stack);
        // 不显示给：非装备 / 非管线内 + 也非合格白板（避免每个原版工具都被 spam）
        // 仅显示给：已在管线内的装备
        if (!inPipeline) return;
        // 注：合格但 tier=0 的"白板"装备不显示（避免噪声；玩家进 E 后才显示）
        // 例外：如果未来要显示"白板可入坩埚"提示，可把这行改为 if (!eligible) return;

        List<Component> lines = event.getToolTip();
        int tier = GearForgeData.getTier(stack);

        // ─── R88: 名称装饰 — 替换 lines[0] 为带 tier 星级前缀 + aspect/blessing 色调
        if (!lines.isEmpty()) {
            lines.set(0, decorateName(lines.get(0), stack, tier));
        }

        // 标题行
        lines.add(Component.translatable("gear.transcend.forge.title", tier, 5)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

        // ─── R90: 5 阶段独立增幅词条 ─────────────────────────────────────

        // E 坩埚 — "灼魂 → +20% 综合伤害"
        appendStageLine(lines, ForgeStage.CRUCIBLE,
                GearForgeData.isStageWritten(stack, ForgeStage.CRUCIBLE),
                () -> buildCrucibleLine(stack));

        // B 共鸣 — "锋锐×2 护佑×1 敏疾×1"（按 socket 类型紧凑列举）
        appendStageLine(lines, ForgeStage.RESONANCE,
                GearForgeData.isStageWritten(stack, ForgeStage.RESONANCE),
                () -> buildResonanceLine(stack));

        // A 灵魂 — "烙印 猪灵×2 → 击中猪灵 +50% 伤害"
        appendStageLine(lines, ForgeStage.SOUL,
                GearForgeData.isStageWritten(stack, ForgeStage.SOUL),
                () -> buildSoulLine(stack));

        // C 经历 — "觉醒 II → +12% 综合战斗增幅"
        appendStageLine(lines, ForgeStage.EXPERIENCE,
                GearForgeData.isStageWritten(stack, ForgeStage.EXPERIENCE),
                () -> buildExperienceLine(stack));

        // D 天命 — "日冕 → +30% 攻击（白昼时再 +20%）"
        appendStageLine(lines, ForgeStage.CELESTIAL,
                GearForgeData.isStageWritten(stack, ForgeStage.CELESTIAL),
                () -> buildCelestialLine(stack));

        // R91: 触发型词条独立行（与 5 阶段独立 — 仅在已铭刻时显示，不显示"未铭刻"占位）
        if (GearForgeData.hasTriggerAffix(stack)) {
            appendTriggerAffixLine(lines, stack);
        }
    }

    /** R91: 触发型词条独立行（带主题色 + 类别标签 + 效果描述）。 */
    private static void appendTriggerAffixLine(List<Component> lines, ItemStack stack) {
        var data = GearForgeData.getTriggerAffix(stack);
        if (data == null) return;
        TriggerAffixKind kind = TriggerAffixKind.byId(data.affixId());
        if (kind == null) {
            lines.add(Component.literal("  ⚡ ").withStyle(ChatFormatting.DARK_GRAY)
                    .copy().append(Component.literal(data.affixId()).withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }

        MutableComponent line = Component.literal("  ⚡ ").withStyle(kind.color);
        line.append(Component.translatable("gear.transcend.forge.trigger.label").withStyle(kind.color, ChatFormatting.BOLD));
        line.append(Component.literal(": ").withStyle(kind.color));
        line.append(Component.translatable(kind.nameKey()).withStyle(kind.color, ChatFormatting.BOLD));
        line.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
        line.append(Component.translatable("trigger_affix.transcend.category." + kind.category.name().toLowerCase())
                .withStyle(ChatFormatting.DARK_GRAY));
        line.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(line);

        // 词条效果描述（小字斜体）
        lines.add(Component.literal("    ").append(
                Component.translatable(kind.descKey()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
    }

    // ─── R90: 各阶段词条构建器 ───────────────────────────────────────

    /** E 坩埚词条：aspect 名 + offset 百分比 + "综合伤害"标签。 */
    private static Component buildCrucibleLine(ItemStack stack) {
        var c = GearForgeData.getCrucible(stack);
        if (c == null) return Component.translatable("gear.transcend.forge.empty.crucible");

        AspectDef aspectDef = AspectRegistry.byId(c.aspect());
        Component aspectName = (aspectDef != null && aspectDef != AspectRegistry.INDETERMINATE)
                ? Component.translatable(aspectDef.nameKey())
                : Component.literal(c.aspect());

        return Component.translatable("gear.transcend.forge.line.crucible",
                aspectName, formatPercent(c.offset()));
    }

    /** B 共鸣词条：紧凑列举每种 socket 类型 + 数量（每种保留其主题色）。 */
    private static Component buildResonanceLine(ItemStack stack) {
        var sockets = GearForgeData.getSockets(stack);
        Map<ResonanceKind, Integer> counts = new EnumMap<>(ResonanceKind.class);
        for (var s : sockets) {
            ResonanceKind k = ResonanceKind.byId(s.crystalId());
            if (k != null) counts.merge(k, 1, Integer::sum);
        }

        // 头部："3/4 ·"
        MutableComponent body = Component.translatable("gear.transcend.forge.line.resonance.count",
                sockets.size(), GearForgeData.MAX_RESONANCE_SOCKETS).copy();

        // 按 enum 自然顺序列出（SHARPNESS, SWIFTNESS, LEECH, WARD, FOCUS, SPARK）
        for (ResonanceKind kind : ResonanceKind.values()) {
            Integer cnt = counts.get(kind);
            if (cnt == null || cnt == 0) continue;
            body.append(Component.literal(" "));
            body.append(Component.translatable(kind.langKey()).withStyle(kind.color));
            body.append(Component.literal("×" + cnt).withStyle(kind.color));
        }
        return body;
    }

    /** A 灵魂词条：每个 mob + echo 数量 + bonus 百分比。 */
    private static Component buildSoulLine(ItemStack stack) {
        var echoes = GearForgeData.getSoulEchoes(stack);
        if (echoes.isEmpty()) {
            return Component.translatable("gear.transcend.forge.line.soul.count",
                    0, GearForgeData.MAX_SOUL_ECHOES);
        }

        // 按 mobId 聚合
        Map<String, Integer> mobCounts = new HashMap<>();
        for (var e : echoes) mobCounts.merge(e.mobId(), 1, Integer::sum);

        MutableComponent body = Component.translatable("gear.transcend.forge.line.soul.count",
                echoes.size(), GearForgeData.MAX_SOUL_ECHOES).copy();

        for (Map.Entry<String, Integer> e : mobCounts.entrySet()) {
            int cnt = e.getValue();
            float bonusPct = cnt * ForgeBattleConfig.SOUL_ECHO_DAMAGE_BONUS * 100f;
            body.append(Component.literal(" "));
            body.append(mobDisplayName(e.getKey()).withStyle(ChatFormatting.WHITE));
            body.append(Component.literal("×" + cnt).withStyle(ChatFormatting.WHITE));
            body.append(Component.literal(" (+").withStyle(ChatFormatting.DARK_GRAY));
            body.append(Component.literal(String.format("%.0f%%", bonusPct))
                    .withStyle(ChatFormatting.GREEN));
            body.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return body;
    }

    /** C 经历词条：tier I/II/III + 对应百分比 综合增幅。仅在 tier&gt;0 时调用（isStageWritten 保证）。 */
    private static Component buildExperienceLine(ItemStack stack) {
        var exp = GearForgeData.getExperience(stack);
        int tier = exp.tier();
        float pct = (tier >= 0 && tier < ForgeBattleConfig.TIER_MULT.length)
                ? ForgeBattleConfig.TIER_MULT[tier] * 100f : 0f;
        Component tierRoman = Component.translatable("gear.transcend.forge.tier." + tier);
        return Component.translatable("gear.transcend.forge.line.experience.awakened",
                tierRoman, String.format("%.0f%%", pct));
    }

    /** D 天命词条：blessing 名 + 基础百分比 + 条件提示。 */
    private static Component buildCelestialLine(ItemStack stack) {
        var c = GearForgeData.getCelestial(stack);
        if (c == null) return Component.translatable("gear.transcend.forge.empty.celestial");

        BlessingDef def = BlessingRegistry.byId(c.blessing());
        Component blessingName = (def != null && def != BlessingRegistry.INDETERMINATE)
                ? Component.translatable(def.nameKey())
                : Component.literal(c.blessing());

        float basePct = (def != null && def.isPure())
                ? ForgeBattleConfig.BLESSING_PURE_BONUS * 100f
                : (def != null ? ForgeBattleConfig.BLESSING_DUAL_BONUS * 100f : 0f);

        MutableComponent body = Component.translatable("gear.transcend.forge.line.celestial",
                blessingName, String.format("%.0f%%", basePct)).copy();

        // 条件提示：solar_crown / lunar_crown
        if (def != null) {
            if ("solar_crown".equals(def.id())) {
                body.append(Component.literal(" "));
                body.append(Component.translatable("gear.transcend.forge.line.celestial.solar_bonus",
                        String.format("%.0f%%", ForgeBattleConfig.SOLAR_DAY_BONUS * 100f))
                        .withStyle(ChatFormatting.YELLOW));
            } else if ("lunar_crown".equals(def.id())) {
                body.append(Component.literal(" "));
                body.append(Component.translatable("gear.transcend.forge.line.celestial.lunar_bonus",
                        String.format("%.0f%%", ForgeBattleConfig.LUNAR_NIGHT_BONUS * 100f))
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        return body;
    }

    // ─── 工具方法 ─────────────────────────────────────────────────────

    /** 把 mobId 字符串 (e.g. "minecraft:zombie") 转成可读的 entity 名 MutableComponent（可链式 withStyle）。 */
    private static MutableComponent mobDisplayName(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return Component.translatable("gear.transcend.forge.mob.unknown");
        }
        try {
            ResourceLocation rl = ResourceLocation.tryParse(mobId);
            if (rl == null) return Component.literal(mobId);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            // EntityType.getDescription() 返回 Component（不可变）；用 copy() 转 MutableComponent
            return type.getDescription().copy();
        } catch (Exception e) {
            return Component.literal(mobId);
        }
    }

    /** 单阶段行：未写入 → 灰色斜体 + ○；已写入 → 阶段色 + ✦/◇。 */
    private static void appendStageLine(List<Component> lines, ForgeStage stage, boolean written,
                                         java.util.function.Supplier<Component> bodySupplier) {
        Component stageName = Component.translatable(stage.getNameKey());
        if (written) {
            Component prefix = Component.literal("  " + stage.marker + " ").withStyle(stage.color);
            Component name   = stageName.copy().withStyle(stage.color, ChatFormatting.BOLD);
            Component body   = bodySupplier.get().copy().withStyle(stage.color);
            lines.add(prefix.copy().append(name).append(Component.literal(": ").withStyle(stage.color)).append(body));
        } else {
            Component prefix = Component.literal("  ○ ").withStyle(ChatFormatting.DARK_GRAY);
            Component name   = stageName.copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            Component body   = Component.translatable("gear.transcend.forge.pending")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            lines.add(prefix.copy().append(name).append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY)).append(body));
        }
    }

    private static String formatPercent(float offset) {
        if (offset == 0) return "+0%";
        return (offset > 0 ? "+" : "") + String.format("%.0f%%", offset * 100f);
    }

    // ─── R88: 名称装饰 ─────────────────────────────────────────────────

    /**
     * R88: 根据 tier + aspect + blessing 生成"传奇"风格的物品名行。
     *
     * <p>规则：
     * <ul>
     *   <li>tier 1: 灰 ◇ + 原名</li>
     *   <li>tier 2: 青 ◆ + 原名（aspect 色覆盖）</li>
     *   <li>tier 3: 蓝 ✦ + 原名（aspect 色 + 加粗）</li>
     *   <li>tier 4: 紫 ★ + 原名（blessing/aspect 色 + 加粗）</li>
     *   <li>tier 5: 金 ✦★ + 原名（金色 + 加粗 + 斜体 = 传奇）</li>
     * </ul>
     *
     * <p>如果有 aspect 或 blessing，会在名字前加 aspect/blessing 名称的简称。
     */
    private static Component decorateName(Component originalName, ItemStack stack, int tier) {
        if (tier <= 0) return originalName;

        String prefix;
        ChatFormatting tierColor;
        boolean bold, italic;
        switch (tier) {
            case 1 -> { prefix = "◇ "; tierColor = ChatFormatting.GRAY;        bold = false; italic = false; }
            case 2 -> { prefix = "◆ "; tierColor = ChatFormatting.AQUA;        bold = false; italic = false; }
            case 3 -> { prefix = "✦ "; tierColor = ChatFormatting.BLUE;        bold = true;  italic = false; }
            case 4 -> { prefix = "★ "; tierColor = ChatFormatting.LIGHT_PURPLE; bold = true;  italic = false; }
            default-> { prefix = "✦★ "; tierColor = ChatFormatting.GOLD;        bold = true;  italic = true;  } // tier ≥ 5
        }

        // aspect 色覆盖 tierColor（如果有 aspect 写入）
        ChatFormatting themeColor = tierColor;
        AspectDef aspectDef = null;
        BlessingDef blessingDef = null;
        GearForgeData.CrucibleData crucible = GearForgeData.getCrucible(stack);
        if (crucible != null) {
            aspectDef = AspectRegistry.byId(crucible.aspect());
            if (aspectDef != null && aspectDef != AspectRegistry.INDETERMINATE) {
                themeColor = aspectDef.dominant().color;
            }
        }
        // blessing 色再次覆盖（tier 4+ 时优先用 blessing 色）
        if (tier >= 4) {
            GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(stack);
            if (bless != null) {
                blessingDef = BlessingRegistry.byId(bless.blessing());
                if (blessingDef != null && blessingDef != BlessingRegistry.INDETERMINATE) {
                    themeColor = blessingDef.dominant().color;
                }
            }
        }

        // 构造 prefix Component
        MutableComponent decorated = Component.literal(prefix).withStyle(tierColor);
        if (bold)   decorated = decorated.withStyle(ChatFormatting.BOLD);
        if (italic) decorated = decorated.withStyle(ChatFormatting.ITALIC);

        // aspect/blessing 短名（嵌入到名字内部）
        if (blessingDef != null) {
            decorated.append(Component.translatable(blessingDef.nameKey())
                    .withStyle(themeColor, ChatFormatting.BOLD));
            decorated.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        } else if (aspectDef != null && tier >= 2) {
            decorated.append(Component.translatable(aspectDef.nameKey())
                    .withStyle(themeColor));
            decorated.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        }

        // 原始名字
        MutableComponent originalCopy = originalName.copy();
        // tier ≥ 3 给原名加粗；tier ≥ 5 加金色覆盖
        if (tier >= 5) {
            originalCopy = Component.empty().append(originalCopy).withStyle(ChatFormatting.GOLD);
        } else if (tier >= 3) {
            originalCopy = Component.empty().append(originalCopy).withStyle(themeColor);
        }
        if (bold)   originalCopy = originalCopy.withStyle(ChatFormatting.BOLD);
        if (italic) originalCopy = originalCopy.withStyle(ChatFormatting.ITALIC);
        decorated.append(originalCopy);

        return decorated;
    }
}
