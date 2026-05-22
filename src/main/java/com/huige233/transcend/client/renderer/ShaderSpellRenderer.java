package com.huige233.transcend.client.renderer;

import com.huige233.transcend.client.circle.CircleGhostClientState;
import com.huige233.transcend.client.circle.OreRevealClientState;
import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import com.huige233.transcend.client.magic.MagicCircleGeometry;
import com.huige233.transcend.network.S2CCircleGhostBlocks.GhostEntry;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ShaderSpellRenderer {

    private static final ResourceLocation GLOW_TEX = new ResourceLocation("transcend", "textures/entity/elemental_warden_glow.png");

    private static final List<ShaderCircleInstance> activeCircles = new ArrayList<>();
    private static final List<ShaderSpellInstance> activeSpells = new ArrayList<>();
    private static final Queue<ShaderCircleInstance> pendingCircles = new ConcurrentLinkedQueue<>();
    private static final Queue<ShaderSpellInstance> pendingSpells = new ConcurrentLinkedQueue<>();

    // === Circle management ===
    public static void addCircle(Vec3 center, float radius, float r, float g, float b,
                                  int lifetime, int segments, String pattern) {
        pendingCircles.offer(new ShaderCircleInstance(center, radius, r, g, b, lifetime, segments, pattern));
    }

    // === Spell effect management ===
    public static void addSpellEffect(Vec3 from, Vec3 to, float r, float g, float b,
                                       int lifetime, String type) {
        pendingSpells.offer(new ShaderSpellInstance(from, to, r, g, b, lifetime, type));
    }

    public static void addShockwave(Vec3 center, float maxRadius, float r, float g, float b, int lifetime) {
        pendingSpells.offer(new ShaderSpellInstance(center, new Vec3(maxRadius, 0, 0), r, g, b, lifetime, "shockwave"));
    }

    public static void addShieldRipple(Vec3 center, float radius, float r, float g, float b, int lifetime) {
        pendingSpells.offer(new ShaderSpellInstance(center, new Vec3(radius, 0, 0), r, g, b, lifetime, "shield_ripple"));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        flushPending();
        boolean hasGhosts = mc.level != null && !CircleGhostClientState.activePreviews(mc.level).isEmpty();
        boolean hasOreReveal = mc.level != null && OreRevealClientState.active(mc.level) != null;
        if (activeCircles.isEmpty() && activeSpells.isEmpty() && !hasGhosts && !hasOreReveal) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        // Render circles
        for (int i = activeCircles.size() - 1; i >= 0; i--) {
            ShaderCircleInstance c = activeCircles.get(i);
            c.age++;
            if (c.age > c.lifetime) {
                activeCircles.remove(i);
                continue;
            }
            renderCircle(ps, buf, c);
        }

        // Render spell effects
        for (int i = activeSpells.size() - 1; i >= 0; i--) {
            ShaderSpellInstance s = activeSpells.get(i);
            s.age++;
            if (s.age > s.lifetime) {
                activeSpells.remove(i);
                continue;
            }
            renderSpell(ps, buf, s);
        }

        // Render ghost blocks for circle structure preview
        renderGhostBlocks(ps, buf, mc);

        // Render ore reveal highlights (Oreblood Revelation scroll)
        renderOreReveal(ps, buf, mc);

        ps.popPose();
        buf.endBatch();
    }

    private static void flushPending() {
        ShaderCircleInstance circle;
        while ((circle = pendingCircles.poll()) != null) {
            activeCircles.add(circle);
        }
        ShaderSpellInstance spell;
        while ((spell = pendingSpells.poll()) != null) {
            activeSpells.add(spell);
        }
    }

    // ============================================================
    // Ghost 方块渲染（法阵结构预览）
    // ============================================================

    private static void renderGhostBlocks(PoseStack ps, MultiBufferSource buf, Minecraft mc) {
        if (mc.level == null) return;
        var previews = CircleGhostClientState.activePreviews(mc.level);
        if (previews.isEmpty()) return;

        VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(GLOW_TEX));

        for (CircleGhostClientState.GhostPreview preview : previews) {
            float pulse = (float) Math.sin(System.currentTimeMillis() * 0.003) * 0.15F + 0.35F;

            for (GhostEntry entry : preview.entries) {
                net.minecraft.core.BlockPos blockPos = entry.pos();
                float x = blockPos.getX();
                float y = blockPos.getY();
                float z = blockPos.getZ();

                // 按 BlockRole 选择颜色
                float r, g, b;
                switch (entry.role()) {
                    case FOUNDATION -> { r = 0.6F; g = 0.4F; b = 0.2F; }  // 棕色 - 基石
                    case RUNE       -> { r = 0.5F; g = 0.3F; b = 1.0F; }  // 紫蓝 - 符文
                    case CONDUIT    -> { r = 0.2F; g = 0.8F; b = 1.0F; }  // 青色 - 导管
                    case PILLAR     -> { r = 0.8F; g = 0.8F; b = 0.3F; }  // 金色 - 柱体
                    case PILLAR_CAP -> { r = 1.0F; g = 0.9F; b = 0.4F; }  // 亮金 - 柱帽
                    case CATALYST_PLINTH -> { r = 1.0F; g = 0.3F; b = 0.3F; }  // 红色 - 催化基座
                    default         -> { r = 0.5F; g = 0.5F; b = 0.5F; }  // 灰色
                }

                float a = pulse;
                int light = 0xF000F0;
                int overlay = OverlayTexture.NO_OVERLAY;

                ps.pushPose();
                ps.translate(x, y, z);
                Matrix4f mat = ps.last().pose();

                // 6 面半透明方块轮廓（略微缩小以避免 z-fight）
                float min = 0.01F;
                float max = 0.99F;

                // 底面 (y=min)
                vc.vertex(mat, min, min, min).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
                vc.vertex(mat, max, min, min).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
                vc.vertex(mat, max, min, max).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
                vc.vertex(mat, min, min, max).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();

                // 顶面 (y=max)
                vc.vertex(mat, min, max, max).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
                vc.vertex(mat, max, max, max).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
                vc.vertex(mat, max, max, min).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
                vc.vertex(mat, min, max, min).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();

                // 北面 (z=min)
                vc.vertex(mat, min, min, min).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
                vc.vertex(mat, min, max, min).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
                vc.vertex(mat, max, max, min).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
                vc.vertex(mat, max, min, min).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();

                // 南面 (z=max)
                vc.vertex(mat, max, min, max).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
                vc.vertex(mat, max, max, max).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
                vc.vertex(mat, min, max, max).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
                vc.vertex(mat, min, min, max).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();

                // 西面 (x=min)
                vc.vertex(mat, min, min, max).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
                vc.vertex(mat, min, max, max).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
                vc.vertex(mat, min, max, min).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
                vc.vertex(mat, min, min, min).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();

                // 东面 (x=max)
                vc.vertex(mat, max, min, min).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
                vc.vertex(mat, max, max, min).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
                vc.vertex(mat, max, max, max).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
                vc.vertex(mat, max, min, max).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();

                ps.popPose();
            }

            // 渲染文字标签（距离 < 16 格时显示）
            renderGhostLabels(ps, buf, mc, preview);
        }
    }

    /** 渲染 Ghost 方块的名称标签（玩家靠近时可见） */
    private static void renderGhostLabels(PoseStack ps, MultiBufferSource buf,
                                           Minecraft mc, CircleGhostClientState.GhostPreview preview) {
        Vec3 playerPos = mc.player.position();
        var font = mc.font;

        for (GhostEntry entry : preview.entries) {
            double dx = entry.pos().getX() + 0.5 - playerPos.x;
            double dy = entry.pos().getY() + 0.5 - playerPos.y;
            double dz = entry.pos().getZ() + 0.5 - playerPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > 16 * 16) continue;  // 超过16格不显示标签

            String label = getRoleLabel(entry.role(), entry.minBlockTier());

            ps.pushPose();
            ps.translate(entry.pos().getX() + 0.5, entry.pos().getY() + 1.2, entry.pos().getZ() + 0.5);
            // 面向玩家
            ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            float scale = 0.025F;
            ps.scale(-scale, -scale, scale);

            int textWidth = font.width(label);
            float textX = -textWidth / 2.0F;
            // 半透明背景 + 文字
            font.drawInBatch(label, textX, 0, 0xFFFFFFFF, false, ps.last().pose(),
                    buf, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0x40000000, 0xF000F0);
            ps.popPose();
        }
    }

    /** 根据 BlockRole 返回本地化后的名称标签 */
    private static String getRoleLabel(BlockRole role, int minTier) {
        String key = switch (role) {
            case FOUNDATION -> "block.transcend.circle_foundation";
            case RUNE -> "block.transcend.circle_rune_stone";
            case CONDUIT -> "block.transcend.leyline_conduit";
            case PILLAR -> "block.transcend.runic_pillar";
            case PILLAR_CAP -> "block.transcend.pillar_cap";
            case CATALYST_PLINTH -> "block.transcend.catalyst_plinth";
            default -> "block.transcend.unknown";
        };
        String name = net.minecraft.network.chat.Component.translatable(key).getString();
        if (minTier > 1) {
            name += " T" + minTier + "+";
        }
        return name;
    }

    // ============================================================
    // 矿石透视渲染（Oreblood Revelation 卷轴）
    // ============================================================

    /** 渲染矿石高亮（半透明彩色立方体覆盖在矿石上） */
    private static void renderOreReveal(PoseStack ps, MultiBufferSource buf, Minecraft mc) {
        if (mc.level == null) return;
        OreRevealClientState.RevealSnapshot snap = OreRevealClientState.active(mc.level);
        if (snap == null || snap.entries.isEmpty()) return;

        // 剩余时长比例 → alpha pulse
        long now = mc.level.getGameTime();
        long left = Math.max(0, snap.expiresAt - now);
        // 临近过期时淡出
        float fadeRatio = Math.min(1.0F, left / 60.0F); // 最后 3 秒淡出

        VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlowXray(GLOW_TEX));
        float pulse = (float) Math.sin(System.currentTimeMillis() * 0.004) * 0.10F + 0.45F;
        float baseAlpha = pulse * fadeRatio;

        int light = 0xF000F0;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (com.huige233.transcend.network.S2COreRevealPack.OreEntry entry : snap.entries) {
            int color = entry.color();
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            net.minecraft.core.BlockPos blockPos = entry.pos();
            float x = blockPos.getX();
            float y = blockPos.getY();
            float z = blockPos.getZ();

            ps.pushPose();
            ps.translate(x, y, z);
            Matrix4f mat = ps.last().pose();

            // 6 面外扩 0.05 让其透过原方块可见
            float min = -0.05F;
            float max = 1.05F;
            float a = baseAlpha;

            // 底
            vc.vertex(mat, min, min, min).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
            vc.vertex(mat, max, min, min).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
            vc.vertex(mat, max, min, max).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
            vc.vertex(mat, min, min, max).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
            // 顶
            vc.vertex(mat, min, max, max).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            vc.vertex(mat, max, max, max).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            vc.vertex(mat, max, max, min).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            vc.vertex(mat, min, max, min).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            // 北
            vc.vertex(mat, min, min, min).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
            vc.vertex(mat, min, max, min).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
            vc.vertex(mat, max, max, min).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
            vc.vertex(mat, max, min, min).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
            // 南
            vc.vertex(mat, max, min, max).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
            vc.vertex(mat, max, max, max).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
            vc.vertex(mat, min, max, max).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
            vc.vertex(mat, min, min, max).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
            // 西
            vc.vertex(mat, min, min, max).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
            vc.vertex(mat, min, max, max).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
            vc.vertex(mat, min, max, min).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
            vc.vertex(mat, min, min, min).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
            // 东
            vc.vertex(mat, max, min, min).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
            vc.vertex(mat, max, max, min).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
            vc.vertex(mat, max, max, max).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
            vc.vertex(mat, max, min, max).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();

            ps.popPose();
        }
    }

    private static void renderCircle(PoseStack ps, MultiBufferSource buf, ShaderCircleInstance c) {
        float progress = (float) c.age / c.lifetime;
        float alpha = progress < 0.1F ? progress * 10 : (progress > 0.8F ? (1 - progress) * 5 : 1.0F);
        alpha *= 0.5F;

        float rotation = c.age * 1.5F;

        VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(GLOW_TEX));
        ps.pushPose();
        ps.translate(c.center.x, c.center.y, c.center.z);

        Matrix4f mat = ps.last().pose();

        // Outer ring
        float lineW = 0.05F;
        drawRing(mat, vc, c.radius, c.radius - lineW, c.segments, rotation,
                c.r, c.g, c.b, alpha);

        // Inner ring
        drawRing(mat, vc, c.radius * 0.6F, c.radius * 0.6F - lineW, c.segments, -rotation * 0.7F,
                c.r * 0.7F, c.g * 0.7F, c.b * 0.7F, alpha * 0.7F);

        // Hexagram lines
        if (c.pattern.equals("hexagram")) {
            drawStar(mat, vc, c.radius * 0.85F, 6, 2, rotation * 0.5F, lineW,
                    c.r, c.g, c.b, alpha * 0.8F);
        } else if (c.pattern.equals("pentagram")) {
            drawStar(mat, vc, c.radius * 0.85F, 5, 2, rotation * 0.5F, lineW,
                    c.r, c.g, c.b, alpha * 0.8F);
        }

        // Radial lines
        drawRadials(mat, vc, c.radius * 0.3F, c.radius * 0.9F, 8, rotation * 0.3F, lineW * 0.7F,
                c.r * 0.5F, c.g * 0.5F, c.b * 0.5F, alpha * 0.4F);

        ps.popPose();
    }

    private static void renderSpell(PoseStack ps, MultiBufferSource buf, ShaderSpellInstance s) {
        float progress = (float) s.age / s.lifetime;
        float alpha = progress < 0.15F ? progress / 0.15F : (1 - progress);
        alpha *= 0.6F;

        VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(GLOW_TEX));
        ps.pushPose();

        Matrix4f mat = ps.last().pose();

        if (s.type.equals("beam")) {
            drawBeamLine(mat, vc, s.from, s.to, 0.15F + (1 - progress) * 0.1F,
                    s.r, s.g, s.b, alpha);
        } else if (s.type.equals("slash")) {
            drawArc(mat, vc, s.from, s.to, 3.0F, 120, 16,
                    s.r, s.g, s.b, alpha);
        } else if (s.type.equals("nova")) {
            float novaR = progress * 5.0F;
            drawRing(mat, vc, novaR, novaR - 0.2F, 32, 0,
                    s.r, s.g, s.b, alpha);
        } else if (s.type.equals("meteor")) {
            Vec3 delta = s.to.subtract(s.from);
            double len = delta.length();
            Vec3 dir = len > 0.001 ? delta.scale(1.0 / len) : new Vec3(0, -1, 0);
            Vec3 cur = s.from.add(delta.scale(progress));
            Vec3 tail = cur.subtract(dir.scale(6.0 + (1 - progress) * 4.5));

            ps.translate(cur.x, cur.y, cur.z);
            mat = ps.last().pose();

            float headR = 1.6F + (1 - progress) * 2.6F;
            drawRing(mat, vc, headR, Math.max(0.1F, headR - 0.24F), 36, progress * 180,
                    s.r, s.g, s.b, alpha);
            drawRing(mat, vc, headR * 0.7F, Math.max(0.05F, headR * 0.7F - 0.16F), 24, -progress * 135,
                    s.r * 0.85F, s.g * 0.85F, s.b * 0.85F, alpha * 0.85F);
            drawVerticalTrail(mat, vc,
                    tail.subtract(cur), Vec3.ZERO,
                    0.22F + (1 - progress) * 0.16F,
                    s.r, s.g, s.b, alpha * 0.8F);
            drawVerticalTrail(mat, vc,
                    tail.subtract(cur).add(dir.scale(1.5)),
                    Vec3.ZERO,
                    0.10F + (1 - progress) * 0.07F,
                    1.0F, 0.85F, 0.35F, alpha * 0.55F);
        } else if (s.type.equals("shockwave")) {
            float maxR = (float) s.to.x;
            float curR = progress * maxR;
            float thick = 0.3F * (1 - progress);
            float shockAlpha = (1 - progress) * 0.8F;
            ps.translate(s.from.x, s.from.y, s.from.z);
            mat = ps.last().pose();
            drawRing(mat, vc, curR, Math.max(0, curR - thick), 48, 0,
                    s.r, s.g, s.b, shockAlpha);
            drawRing(mat, vc, curR * 0.7F, Math.max(0, curR * 0.7F - thick * 0.5F), 48, progress * 90,
                    s.r * 0.8F, s.g * 0.8F, s.b * 0.8F, shockAlpha * 0.4F);
        } else if (s.type.equals("shield_ripple")) {
            float baseR = (float) s.to.x;
            float rippleR = baseR * (0.5F + progress * 0.5F);
            float rippleAlpha = (1 - progress) * 0.6F;
            ps.translate(s.from.x, s.from.y, s.from.z);
            mat = ps.last().pose();
            drawRing(mat, vc, rippleR + 0.1F, rippleR - 0.1F, 32, progress * 180,
                    s.r, s.g, s.b, rippleAlpha);
        } else if (s.type.equals("pillar")) {
            // Round 41: 垂直光柱 — 从 from 到 to 的双正交垂直 quad（"+" 形从上俯视可见）
            // 根部 + 顶部加发光环增强视觉
            float pillarAlpha = alpha * 1.4F;
            float pulse = 0.6F + 0.4F * (float) Math.sin(s.age * 0.4);
            float widthBase = 0.35F;
            float widthFade = widthBase * (1 - progress * 0.5F);
            drawPillar(mat, vc, s.from, s.to, widthFade, s.r, s.g, s.b, pillarAlpha * pulse);
            // 根部光环（地面冲击点）
            ps.pushPose();
            ps.translate(s.to.x, s.to.y + 0.05, s.to.z);
            Matrix4f rootMat = ps.last().pose();
            float ringR = 0.6F + progress * 0.8F;
            drawRing(rootMat, vc, ringR, Math.max(0.05F, ringR - 0.15F), 24, progress * 90,
                    s.r, s.g, s.b, alpha * (1 - progress));
            ps.popPose();
        }

        ps.popPose();
    }

    // === Geometry helpers ===

    private static void drawRing(Matrix4f mat, VertexConsumer vc, float outerR, float innerR,
                                  int segments, float rotation, float r, float g, float b, float a) {
        for (int i = 0; i < segments; i++) {
            float a1 = (float)(Math.PI * 2 * i / segments) + (float) Math.toRadians(rotation);
            float a2 = (float)(Math.PI * 2 * (i + 1) / segments) + (float) Math.toRadians(rotation);

            float x1o = (float) Math.cos(a1) * outerR, z1o = (float) Math.sin(a1) * outerR;
            float x2o = (float) Math.cos(a2) * outerR, z2o = (float) Math.sin(a2) * outerR;
            float x1i = (float) Math.cos(a1) * innerR, z1i = (float) Math.sin(a1) * innerR;
            float x2i = (float) Math.cos(a2) * innerR, z2i = (float) Math.sin(a2) * innerR;

            emitQuad(mat, vc, x1o, 0.01F, z1o, x2o, 0.01F, z2o,
                    x2i, 0.01F, z2i, x1i, 0.01F, z1i, r, g, b, a);
        }
    }

    private static void drawStar(Matrix4f mat, VertexConsumer vc, float radius, int points,
                                  int skip, float rotation, float lineW,
                                  float r, float g, float b, float a) {
        for (int i = 0; i < points; i++) {
            float a1 = (float)(Math.PI * 2 * i / points) + (float) Math.toRadians(rotation);
            float a2 = (float)(Math.PI * 2 * ((i + skip) % points) / points) + (float) Math.toRadians(rotation);

            float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius, z2 = (float) Math.sin(a2) * radius;

            drawLineQuad(mat, vc, x1, 0.02F, z1, x2, 0.02F, z2, lineW, r, g, b, a);
        }
    }

    private static void drawRadials(Matrix4f mat, VertexConsumer vc, float innerR, float outerR,
                                     int count, float rotation, float lineW,
                                     float r, float g, float b, float a) {
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.PI * 2 * i / count) + (float) Math.toRadians(rotation);
            float x1 = (float) Math.cos(angle) * innerR, z1 = (float) Math.sin(angle) * innerR;
            float x2 = (float) Math.cos(angle) * outerR, z2 = (float) Math.sin(angle) * outerR;
            drawLineQuad(mat, vc, x1, 0.02F, z1, x2, 0.02F, z2, lineW, r, g, b, a);
        }
    }

    private static void drawBeamLine(Matrix4f mat, VertexConsumer vc, Vec3 from, Vec3 to,
                                      float width, float r, float g, float b, float a) {
        float dx = (float)(to.x - from.x), dz = (float)(to.z - from.z);
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01F) return;
        float nx = -dz / len * width, nz = dx / len * width;

        emitQuad(mat, vc,
                (float) from.x + nx, (float) from.y + 0.1F, (float) from.z + nz,
                (float) from.x - nx, (float) from.y + 0.1F, (float) from.z - nz,
                (float) to.x - nx * 0.3F, (float) to.y + 0.1F, (float) to.z - nz * 0.3F,
                (float) to.x + nx * 0.3F, (float) to.y + 0.1F, (float) to.z + nz * 0.3F,
                r, g, b, a);
    }

    private static void drawArc(Matrix4f mat, VertexConsumer vc, Vec3 center, Vec3 look,
                                 float radius, float degrees, int segments,
                                 float r, float g, float b, float a) {
        float baseAngle = (float) Math.atan2(look.z - center.z, look.x - center.x);
        float halfArc = (float) Math.toRadians(degrees / 2);
        float lineW = 0.08F;

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float ang0 = baseAngle - halfArc + t0 * halfArc * 2;
            float ang1 = baseAngle - halfArc + t1 * halfArc * 2;

            float x0 = (float)(center.x + Math.cos(ang0) * radius);
            float z0 = (float)(center.z + Math.sin(ang0) * radius);
            float x1 = (float)(center.x + Math.cos(ang1) * radius);
            float z1 = (float)(center.z + Math.sin(ang1) * radius);

            drawLineQuad(mat, vc, x0, (float) center.y + 0.5F, z0,
                    x1, (float) center.y + 0.5F, z1, lineW, r, g, b, a * (1 - t0 * 0.3F));
        }
    }

    private static void drawLineQuad(Matrix4f mat, VertexConsumer vc,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001F) return;
        float nx = -dz / len * width, nz = dx / len * width;

        emitQuad(mat, vc,
                x1 + nx, y1, z1 + nz,
                x1 - nx, y1, z1 - nz,
                x2 - nx, y2, z2 - nz,
                x2 + nx, y2, z2 + nz,
                r, g, b, a);
    }

    private static void drawVerticalTrail(Matrix4f mat, VertexConsumer vc,
                                          Vec3 from, Vec3 to, float width,
                                          float r, float g, float b, float a) {
        emitQuad(mat, vc,
                (float) from.x - width, (float) from.y, (float) from.z - width,
                (float) from.x + width, (float) from.y, (float) from.z + width,
                (float) to.x + width, (float) to.y, (float) to.z + width,
                (float) to.x - width, (float) to.y, (float) to.z - width,
                r, g, b, a);
    }

    private static void emitQuad(Matrix4f mat, VertexConsumer vc,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float r, float g, float b, float a) {
        vc.vertex(mat, x0, y0, z0).color(r, g, b, a)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 1, 0).endVertex();
        vc.vertex(mat, x1, y1, z1).color(r, g, b, a)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 1, 0).endVertex();
        vc.vertex(mat, x2, y2, z2).color(r, g, b, a)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 1, 0).endVertex();
        vc.vertex(mat, x3, y3, z3).color(r, g, b, a)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 1, 0).endVertex();
    }

    /**
     * Round 41: 垂直光柱（双正交 quad）— 形成 "+" 截面的发光柱体。
     * 用于 attack scroll 命中视觉：日裁天罚（黄柱）/ 地脉喷发（青柱）等。
     */
    private static void drawPillar(Matrix4f mat, VertexConsumer vc, Vec3 top, Vec3 bottom,
                                    float width, float r, float g, float b, float a) {
        float topX = (float) top.x, topY = (float) top.y, topZ = (float) top.z;
        float botX = (float) bottom.x, botY = (float) bottom.y, botZ = (float) bottom.z;

        // Quad A: 法线沿 Z 轴（北南视角可见）
        emitQuad(mat, vc,
                botX - width, botY, botZ,
                botX + width, botY, botZ,
                topX + width, topY, topZ,
                topX - width, topY, topZ,
                r, g, b, a);

        // Quad B: 法线沿 X 轴（东西视角可见）
        emitQuad(mat, vc,
                botX, botY, botZ - width,
                botX, botY, botZ + width,
                topX, topY, topZ + width,
                topX, topY, topZ - width,
                r, g, b, a);

        // 内核更亮的细柱（增强中心发光感）
        float coreW = width * 0.35F;
        float coreA = Math.min(1.0F, a * 1.5F);
        emitQuad(mat, vc,
                botX - coreW, botY, botZ,
                botX + coreW, botY, botZ,
                topX + coreW, topY, topZ,
                topX - coreW, topY, topZ,
                Math.min(1.0F, r + 0.3F), Math.min(1.0F, g + 0.3F), Math.min(1.0F, b + 0.3F), coreA);
        emitQuad(mat, vc,
                botX, botY, botZ - coreW,
                botX, botY, botZ + coreW,
                topX, topY, topZ + coreW,
                topX, topY, topZ - coreW,
                Math.min(1.0F, r + 0.3F), Math.min(1.0F, g + 0.3F), Math.min(1.0F, b + 0.3F), coreA);
    }

    // === Data classes ===
    private static class ShaderCircleInstance {
        final Vec3 center;
        final float radius, r, g, b;
        final int lifetime, segments;
        final String pattern;
        int age = 0;

        ShaderCircleInstance(Vec3 center, float radius, float r, float g, float b,
                             int lifetime, int segments, String pattern) {
            this.center = center; this.radius = radius;
            this.r = r; this.g = g; this.b = b;
            this.lifetime = lifetime; this.segments = segments; this.pattern = pattern;
        }
    }

    private static class ShaderSpellInstance {
        final Vec3 from, to;
        final float r, g, b;
        final int lifetime;
        final String type;
        int age = 0;

        ShaderSpellInstance(Vec3 from, Vec3 to, float r, float g, float b, int lifetime, String type) {
            this.from = from; this.to = to;
            this.r = r; this.g = g; this.b = b;
            this.lifetime = lifetime; this.type = type;
        }
    }
}
