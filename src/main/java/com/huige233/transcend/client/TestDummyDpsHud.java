package com.huige233.transcend.client;

import com.huige233.transcend.entity.TestDummy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

   
                        
                               
                       
                                    
   
/** 追踪准星目标或附近战斗中的假人，平滑显示减免前后伤害、累计伤害、秒伤与护盾。 */
@Mod.EventBusSubscriber(modid = "transcend", value = Dist.CLIENT)
public final class TestDummyDpsHud {

    private TestDummyDpsHud() {
    }

    
    private static float shownRaw, shownReduced, shownTotal, shownDps;
    
    private static TestDummy trackedDummy;
    
    private static float lastServerTotal = -1;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        TestDummy dummy = findDummy(mc);
        if (dummy == null) {
            resetInterpolation();
            return;
        }
        
        if (dummy != trackedDummy) {
            trackedDummy = dummy;
            resetInterpolation();
        }

        
        float raw = dummy.getRawDamage();
        float reduced = dummy.getLastDamage();
        float total = dummy.getTotalDamage();
        float dps = dummy.getBurstTicks() > 0 ? dummy.getBurstDamage() / 5.0F : 0;

        
        if (total < lastServerTotal && lastServerTotal >= 0) {
            shownRaw = raw;
            shownReduced = reduced;
            shownTotal = total;
            shownDps = dps;
        }
        lastServerTotal = total;

        
        shownRaw = lerp(shownRaw, raw);
        shownReduced = lerp(shownReduced, reduced);
        shownTotal = lerp(shownTotal, total);
        shownDps = lerp(shownDps, dps);

        Font font = mc.font;
        var gui = event.getGuiGraphics();
        int cx = gui.guiWidth() / 2;
        int cy = gui.guiHeight() / 2;

        String lastLine;
        if (shownRaw > 0 && Math.abs(shownRaw - shownReduced) > 0.5F) {
            lastLine = String.format("§7伤害 §c%.1f §8→ §a%.1f", shownRaw, shownReduced);
        } else {
            lastLine = String.format("§7伤害 §c%.1f", shownReduced);
        }
        String line2 = String.format("§7总伤 %s   %s%s/s",
                fmt(shownTotal),
                ChatFormatting.GOLD.toString(), fmt(shownDps));
        
        TestDummy.DamageCategory cat = dummy.getLastDamage() > 0 || dummy.getHitCount() > 0
                ? dummy.getLastCategory() : null;
        String lineCat = cat != null
                ? String.format("§7类型 §f%s", categoryName(cat))
                : "§8类型 §7-";
        String line3 = String.format("§8最高 %s §8| 受击 %d",
                fmt(dummy.getMaxHit()), dummy.getHitCount());
        
        boolean shieldOn = dummy.isShieldEnabled();
        String lineShield = shieldOn
                ? String.format("§b护盾 %s §7/ %s", fmt(dummy.getShieldValue()), fmt(dummy.getShieldMax()))
                : null;

        java.util.List<String> lines = new java.util.ArrayList<>(
                java.util.List.of(lastLine, lineCat, line2, line3));
        if (lineShield != null) lines.add(1, lineShield);

        int w = 0;
        for (String l : lines) w = Math.max(w, font.width(l));
        w += 12;
        int h = lines.size() * 11 + 9;
        int x0 = cx - w / 2;
        int y0 = cy + 16;

        gui.fill(x0, y0, x0 + w, y0 + h, 0x90000000);
        gui.fill(x0, y0, x0 + w, y0 + 1, 0x90CC8844);
        int ty = y0 + 4;
        for (String l : lines) {
            gui.drawString(font, Component.literal(l), x0 + 6, ty, 0xFFFFFF, true);
            ty += 11;
        }
    }

    
    private static String categoryName(TestDummy.DamageCategory cat) {
        return switch (cat) {
            case MELEE -> "§c近战";
            case PROJECTILE -> "§e投射";
            case EXPLOSION -> "§6爆炸";
            case FIRE -> "§4火焰";
            case MAGIC -> "§5魔法";
            case ENVIRONMENT -> "§b环境";
        };
    }

    
    private static float lerp(float current, float target) {
        if (Math.abs(target - current) < 0.05F) return target;
        return Mth.lerp(step(), current, target);
    }

    
    private static void resetInterpolation() {
        shownRaw = 0;
        shownReduced = 0;
        shownTotal = 0;
        shownDps = 0;
        lastServerTotal = -1;
    }

    private static float step() {
        return 0.35F;
    }

    
    private static TestDummy findDummy(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof TestDummy d) {
            return d;
        }
        if (mc.player == null) return null;
        TestDummy best = null;
        double bestDist = 32 * 32;
        for (var e : mc.player.level().getEntitiesOfClass(TestDummy.class,
                mc.player.getBoundingBox().inflate(32))) {
            if (!e.isBurstWindowActive()) continue;
            double dist = e.distanceToSqr(mc.player);
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    
    private static String fmt(float v) {
        if (v >= 1_000_000F) return String.format("%.2fM", v / 1_000_000F);
        if (v >= 10_000F) return String.format("%.1fk", v / 1_000F);
        return String.format("%.1f", v);
    }
}
