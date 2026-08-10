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

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 装备锻造 tooltip 工具类。 */
public class GearForgeTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        boolean inPipeline = GearForgeData.isInPipeline(stack);
        boolean eligible   = GearForgeData.isEligibleForPipeline(stack);

        if (!inPipeline) return;

        List<Component> lines = event.getToolTip();
        int tier = GearForgeData.getTier(stack);

        if (!lines.isEmpty()) {
            lines.set(0, decorateName(lines.get(0), stack, tier));
        }

        lines.add(Component.translatable("gear.transcend.forge.title", tier, 5)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

        appendStageLine(lines, ForgeStage.CRUCIBLE,
                GearForgeData.isStageWritten(stack, ForgeStage.CRUCIBLE),
                () -> buildCrucibleLine(stack));

        appendStageLine(lines, ForgeStage.RESONANCE,
                GearForgeData.isStageWritten(stack, ForgeStage.RESONANCE),
                () -> buildResonanceLine(stack));

        appendStageLine(lines, ForgeStage.SOUL,
                GearForgeData.isStageWritten(stack, ForgeStage.SOUL),
                () -> buildSoulLine(stack));

        appendStageLine(lines, ForgeStage.EXPERIENCE,
                GearForgeData.isStageWritten(stack, ForgeStage.EXPERIENCE),
                () -> buildExperienceLine(stack));

        appendStageLine(lines, ForgeStage.CELESTIAL,
                GearForgeData.isStageWritten(stack, ForgeStage.CELESTIAL),
                () -> buildCelestialLine(stack));

        if (GearForgeData.hasTriggerAffix(stack)) {
            appendTriggerAffixLine(lines, stack);
        }
    }

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

        lines.add(Component.literal("    ").append(
                Component.translatable(kind.descKey()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
    }

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

    private static Component buildResonanceLine(ItemStack stack) {
        var sockets = GearForgeData.getSockets(stack);
        Map<ResonanceKind, Integer> counts = new EnumMap<>(ResonanceKind.class);
        for (var s : sockets) {
            ResonanceKind k = ResonanceKind.byId(s.crystalId());
            if (k != null) counts.merge(k, 1, Integer::sum);
        }

        MutableComponent body = Component.translatable("gear.transcend.forge.line.resonance.count",
                sockets.size(), GearForgeData.MAX_RESONANCE_SOCKETS).copy();

        for (ResonanceKind kind : ResonanceKind.values()) {
            Integer cnt = counts.get(kind);
            if (cnt == null || cnt == 0) continue;
            body.append(Component.literal(" "));
            body.append(Component.translatable(kind.langKey()).withStyle(kind.color));
            body.append(Component.literal("×" + cnt).withStyle(kind.color));
        }
        return body;
    }

    private static Component buildSoulLine(ItemStack stack) {
        var echoes = GearForgeData.getSoulEchoes(stack);
        if (echoes.isEmpty()) {
            return Component.translatable("gear.transcend.forge.line.soul.count",
                    0, GearForgeData.MAX_SOUL_ECHOES);
        }

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

    private static Component buildExperienceLine(ItemStack stack) {
        var exp = GearForgeData.getExperience(stack);
        int tier = exp.tier();
        float pct = (tier >= 0 && tier < ForgeBattleConfig.TIER_MULT.length)
                ? ForgeBattleConfig.TIER_MULT[tier] * 100f : 0f;
        Component tierRoman = Component.translatable("gear.transcend.forge.tier." + tier);
        return Component.translatable("gear.transcend.forge.line.experience.awakened",
                tierRoman, String.format("%.0f%%", pct));
    }

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

    private static MutableComponent mobDisplayName(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return Component.translatable("gear.transcend.forge.mob.unknown");
        }
        try {
            ResourceLocation rl = ResourceLocation.tryParse(mobId);
            if (rl == null) return Component.literal(mobId);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);

            return type.getDescription().copy();
        } catch (Exception e) {
            return Component.literal(mobId);
        }
    }

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
            default-> { prefix = "✦★ "; tierColor = ChatFormatting.GOLD;        bold = true;  italic = true;  }
        }

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

        if (tier >= 4) {
            GearForgeData.CelestialBlessing bless = GearForgeData.getCelestial(stack);
            if (bless != null) {
                blessingDef = BlessingRegistry.byId(bless.blessing());
                if (blessingDef != null && blessingDef != BlessingRegistry.INDETERMINATE) {
                    themeColor = blessingDef.dominant().color;
                }
            }
        }

        MutableComponent decorated = Component.literal(prefix).withStyle(tierColor);
        if (bold)   decorated = decorated.withStyle(ChatFormatting.BOLD);
        if (italic) decorated = decorated.withStyle(ChatFormatting.ITALIC);

        if (blessingDef != null) {
            decorated.append(Component.translatable(blessingDef.nameKey())
                    .withStyle(themeColor, ChatFormatting.BOLD));
            decorated.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        } else if (aspectDef != null && tier >= 2) {
            decorated.append(Component.translatable(aspectDef.nameKey())
                    .withStyle(themeColor));
            decorated.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        }

        MutableComponent originalCopy = originalName.copy();

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
