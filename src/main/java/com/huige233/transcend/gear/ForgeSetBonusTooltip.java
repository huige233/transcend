package com.huige233.transcend.gear;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.forge.AspectDef;
import com.huige233.transcend.gear.forge.AspectKind;
import com.huige233.transcend.gear.forge.AspectRegistry;
import com.huige233.transcend.gear.forge.BlessingDef;
import com.huige233.transcend.gear.forge.BlessingRegistry;
import com.huige233.transcend.gear.forge.CelestialKind;
import com.huige233.transcend.gear.forge.ResonanceKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * R92: 造物之道套装协同 tooltip — 在 forged 装备的 tooltip 末尾追加当前激活的套装效果。
 *
 * <p>仅 client side（依赖 {@link Minecraft#player}）。
 *
 * <h2>显示规则</h2>
 * <ul>
 *   <li>仅在 hover forged 装备时检查（避免每件原版工具被 spam）</li>
 *   <li>扫描当前玩家穿戴的 5 槽（MAINHAND + 4 ARMOR）</li>
 *   <li>列出所有激活的套装协同（aspect / socket / blessing 3 维）</li>
 *   <li>未激活任何套装时不显示 R92 行（无 clutter）</li>
 * </ul>
 *
 * <h2>显示格式</h2>
 * <pre>
 *   ❖ 套装协同:
 *     ✦ 烈焰之拥 (火 ×4)
 *     ✦ 锋锐之极 (SHARPNESS ×8)
 *     ✦ 日轮加冕 (SUN ×2, 白昼)
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeSetBonusTooltip {

    private static final int ASPECT_THRESHOLD = 4;
    private static final int SOCKET_THRESHOLD = 8;
    private static final int BLESSING_THRESHOLD = 2;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!GearForgeData.isInPipeline(stack)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // 扫描玩家穿戴
        Map<AspectKind, Integer> aspectCounts = new EnumMap<>(AspectKind.class);
        Map<ResonanceKind, Integer> socketCounts = new EnumMap<>(ResonanceKind.class);
        Map<CelestialKind, Integer> blessingCounts = new EnumMap<>(CelestialKind.class);

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack equipped = player.getItemBySlot(slot);
            if (equipped.isEmpty() || !GearForgeData.isInPipeline(equipped)) continue;

            var crucible = GearForgeData.getCrucible(equipped);
            if (crucible != null) {
                AspectDef def = AspectRegistry.byId(crucible.aspect());
                if (def != null && def != AspectRegistry.INDETERMINATE) {
                    aspectCounts.merge(def.dominant(), 1, Integer::sum);
                }
            }
            for (var sock : GearForgeData.getSockets(equipped)) {
                ResonanceKind k = ResonanceKind.byId(sock.crystalId());
                if (k != null) socketCounts.merge(k, 1, Integer::sum);
            }
            var blessing = GearForgeData.getCelestial(equipped);
            if (blessing != null) {
                BlessingDef bd = BlessingRegistry.byId(blessing.blessing());
                if (bd != null && bd != BlessingRegistry.INDETERMINATE) {
                    blessingCounts.merge(bd.dominant(), 1, Integer::sum);
                }
            }
        }

        List<Component> lines = event.getToolTip();
        boolean headerWritten = false;

        // Aspect 套装
        for (AspectKind ak : AspectKind.values()) {
            int n = aspectCounts.getOrDefault(ak, 0);
            if (n < ASPECT_THRESHOLD) continue;
            if (!headerWritten) {
                lines.add(Component.translatable("gear.transcend.forge.set.header")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                headerWritten = true;
            }
            lines.add(Component.literal("  ✦ ").withStyle(ak.color)
                    .append(Component.translatable("gear.transcend.forge.set.aspect." + ak.id)
                            .withStyle(ak.color, ChatFormatting.BOLD))
                    .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(ak.langKey()).withStyle(ak.color))
                    .append(Component.literal(" ×" + n + ")").withStyle(ChatFormatting.DARK_GRAY)));
        }

        // Socket 套装
        for (ResonanceKind rk : ResonanceKind.values()) {
            int n = socketCounts.getOrDefault(rk, 0);
            if (n < SOCKET_THRESHOLD) continue;
            if (!headerWritten) {
                lines.add(Component.translatable("gear.transcend.forge.set.header")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                headerWritten = true;
            }
            lines.add(Component.literal("  ✦ ").withStyle(rk.color)
                    .append(Component.translatable("gear.transcend.forge.set.socket." + rk.id)
                            .withStyle(rk.color, ChatFormatting.BOLD))
                    .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(rk.langKey()).withStyle(rk.color))
                    .append(Component.literal(" ×" + n + ")").withStyle(ChatFormatting.DARK_GRAY)));
        }

        // Blessing 套装
        for (CelestialKind ck : CelestialKind.values()) {
            int n = blessingCounts.getOrDefault(ck, 0);
            if (n < BLESSING_THRESHOLD) continue;
            if (!headerWritten) {
                lines.add(Component.translatable("gear.transcend.forge.set.header")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                headerWritten = true;
            }

            // 显示条件激活状态（SUN→白昼 / MOON→夜晚 / ABYSS→水中）
            String condKey = null;
            boolean condActive = false;
            switch (ck) {
                case SUN  -> { condKey = "day";   condActive = player.level().isDay(); }
                case MOON -> { condKey = "night"; condActive = player.level().isNight(); }
                case ABYSS-> { condKey = "water"; condActive = player.isInWater(); }
                case STAR -> { condKey = null;    condActive = true; }
            }

            var line = Component.literal("  ✦ ").withStyle(ck.color)
                    .append(Component.translatable("gear.transcend.forge.set.blessing." + ck.id)
                            .withStyle(ck.color, ChatFormatting.BOLD))
                    .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(ck.langKey()).withStyle(ck.color))
                    .append(Component.literal(" ×" + n).withStyle(ChatFormatting.DARK_GRAY));

            if (condKey != null) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                line.append(Component.translatable("gear.transcend.forge.set.cond." + condKey)
                        .withStyle(condActive ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            line.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(line);
        }
    }
}
