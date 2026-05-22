package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.TranscendWand;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SpellHudOverlay {

    private static final ResourceLocation SPELL_BAR = new ResourceLocation(Transcend.MODID, "textures/gui/spell_bar.png");
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_INNER = 18;
    private static final int SLOT_GAP = 2;
    private static final int CD_SEGMENTS = 24;

    private static float pulseTimer = 0;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof TranscendWand)) return;

        CompoundTag tag = mainHand.getTag();
        if (tag == null || !tag.contains("wand_slots", Tag.TAG_LIST)) return;

        ListTag slots = tag.getList("wand_slots", Tag.TAG_COMPOUND);
        int selected = tag.getInt("selected_slot");
        int maxSlots = tag.getInt("max_slots");
        if (maxSlots <= 0) maxSlots = 3;

        GuiGraphics gui = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        pulseTimer += 0.05F;

        int totalWidth = maxSlots * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int startX = screenW / 2 - totalWidth / 2;
        int startY = screenH - 64;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int panelPad = 5;
        drawDarkPanel(gui, startX - panelPad - 1, startY - panelPad,
                startX + totalWidth + panelPad + 1, startY + SLOT_SIZE + panelPad + 1);

        for (int i = 0; i < maxSlots && i < slots.size(); i++) {
            int sx = startX + i * (SLOT_SIZE + SLOT_GAP);
            int sy = startY;
            boolean isSel = (i == selected);
            CompoundTag slotData = slots.getCompound(i);
            boolean occupied = slotData.contains("carrier");

            if (isSel) {
                float glow = 0.4F + 0.25F * (float) Math.sin(pulseTimer * 2.5);
                int glowAlpha = (int) (glow * 255);
                drawGlowRect(gui, sx - 2, sy - 2, sx + SLOT_SIZE + 2, sy + SLOT_SIZE + 2,
                        (glowAlpha << 24) | 0xC8A000);
                gui.blit(SPELL_BAR, sx, sy, 24, 0, SLOT_SIZE, SLOT_SIZE, 256, 256);
            } else {
                gui.blit(SPELL_BAR, sx, sy, 0, 0, SLOT_SIZE, SLOT_SIZE, 256, 256);
            }

            if (occupied) {
                SpellElement element = SpellElement.getById(slotData.getString("element"));
                int eColor = toArgb(0.85F, element.getParticleR(), element.getParticleG(), element.getParticleB());

                ResourceLocation elemIcon = new ResourceLocation(Transcend.MODID,
                        "textures/gui/elements/" + element.id + ".png");
                gui.blit(elemIcon, sx + 4, sy + 4, 0, 0, 16, 16, 16, 16);

                int cd = tag.getInt("slot_cd_" + i);
                if (cd > 0) {
                    SpellCarrier carrier = SpellCarrier.getById(slotData.getString("carrier"));
                    float baseCd = slotData.contains("base_cooldown") ? slotData.getFloat("base_cooldown") : 1.0F;
                    int maxCd = Math.max(1, (int) (carrier.getBaseCooldown() * baseCd));
                    float ratio = Math.min(1.0F, (float) cd / maxCd);

                    drawCooldownSweep(gui, sx + SLOT_SIZE / 2, sy + SLOT_SIZE / 2,
                            SLOT_SIZE / 2 - 1, ratio, 0xBB000000);
                }

                int level = slotData.getInt("spell_level");
                if (level > 1) {
                    String lvl = String.valueOf(level);
                    gui.drawString(mc.font, lvl,
                            sx + SLOT_SIZE - mc.font.width(lvl) - 2, sy + SLOT_SIZE - 10,
                            0xFFFFD700, true);
                }
            } else {
                gui.fill(sx + 9, sy + 9, sx + 15, sy + 15, 0xFF1A1A28);
            }
        }

        // ── 法力条已移除 (v10) ─────────────────────────────────────────
        // 原因: 玩家反馈持法杖时显示的 CM 法力条多余 — 内在魔力 HUD
        // (InnateManaHudOverlay) 已经在左上角显示更完整的信息,
        // 这里的总 mana 条与之重复,且让 HUD 显得拥挤。
        // 仅保留：法术槽、冷却扫描、选中法术名称、消耗 / 等级提示。

        // ── 选中法术名 + 消耗信息 (法术槽下方,无需法力条) ──────────────
        CompoundTag selectedData = selected < slots.size() ? slots.getCompound(selected) : new CompoundTag();
        if (selectedData.contains("carrier")) {
            SpellCarrier carrier = SpellCarrier.getById(selectedData.getString("carrier"));
            SpellElement element = SpellElement.getById(selectedData.getString("element"));
            SpellEffect effect = SpellEffect.getById(selectedData.getString("effect"));

            String carrierName = Component.translatable(carrier.getDisplayKey()).getString();
            String elementName = Component.translatable(element.getDisplayKey()).getString();
            String spellName = carrierName + " · " + elementName;
            if (effect != null) {
                spellName += " · " + Component.translatable(effect.getDisplayKey()).getString();
            }

            int nameW = mc.font.width(spellName);
            int nameX = screenW / 2 - nameW / 2;
            // 法术名上移至法术槽正上方 (原本在法力条上方)
            int nameY = startY - panelPad - 12;

            gui.fill(nameX - 4, nameY - 2, nameX + nameW + 4, nameY + 10, 0x90080614);
            int elColor = toArgb(1.0F, element.getParticleR(), element.getParticleG(), element.getParticleB());
            gui.drawString(mc.font, spellName, nameX, nameY, elColor, true);

            int cost = TranscendWand.getManaCostFromTag(selectedData);
            int level = TranscendWand.getSpellLevel(mainHand, selected);
            String infoStr = "◆" + cost + "  Lv." + level;
            int infoW = mc.font.width(infoStr);
            gui.drawString(mc.font, infoStr, screenW / 2 - infoW / 2,
                    startY + SLOT_SIZE + panelPad + 3, 0xFF8899BB, true);
        }

        RenderSystem.disableBlend();
    }

    private static void drawDarkPanel(GuiGraphics gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xD0080614);
        gui.fill(x1 + 1, y1, x2 - 1, y1 + 1, 0xC0100C22);
        gui.fill(x1 + 1, y2 - 1, x2 - 1, y2, 0xC0100C22);
        gui.fill(x1, y1 + 1, x1 + 1, y2 - 1, 0xC0100C22);
        gui.fill(x2 - 1, y1 + 1, x2, y2 - 1, 0xC0100C22);
        drawBorder1px(gui, x1, y1, x2, y2, 0xFF201838);
    }

    private static void drawBorder1px(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        gui.fill(x1, y1, x2, y1 + 1, color);
        gui.fill(x1, y2 - 1, x2, y2, color);
        gui.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        gui.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    private static void drawGlowRect(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        int a = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        drawBorder1px(gui, x1, y1, x2, y2, (a << 24) | rgb);
        drawBorder1px(gui, x1 - 1, y1 - 1, x2 + 1, y2 + 1, ((a * 2 / 3) << 24) | rgb);
    }

    private static void drawCooldownSweep(GuiGraphics gui, int cx, int cy, int radius,
                                           float ratio, int color) {
        if (ratio <= 0) return;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float endAngle = (float) (Math.PI * 2.0 * ratio);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f mat = gui.pose().last().pose();
        buf.vertex(mat, cx, cy, 0).color(r, g, b, a).endVertex();

        for (int i = 0; i <= CD_SEGMENTS; i++) {
            float angle = -((float) Math.PI / 2) + endAngle * i / CD_SEGMENTS;
            float px = cx + (float) Math.cos(angle) * radius;
            float py = cy + (float) Math.sin(angle) * radius;
            buf.vertex(mat, px, py, 0).color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(buf.end());
    }

    private static int toArgb(float a, float r, float g, float b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255)));
    }
}
