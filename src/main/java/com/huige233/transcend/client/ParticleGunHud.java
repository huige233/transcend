package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.tech.ParticleGun;
import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.ammo.AmmoType;
import com.huige233.transcend.tech.ammo.BuiltInAmmoTypes;
import com.huige233.transcend.tech.ammo.Magazine;
import com.huige233.transcend.tech.attribute.AttributeContainer;
import com.huige233.transcend.tech.attribute.TechAttribute;
import com.huige233.transcend.tech.core.TechItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** 读取主手粒子枪状态，并在屏幕叠加显示弹药数量、充能、热量与过热警告。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT)
public final class ParticleGunHud {
    private static final int PANEL_WIDTH = 172;
    private static final int PANEL_HEIGHT = 78;
    private static final int MARGIN = 8;
    private static final int HOTBAR_CLEARANCE = 62;
    private static ParticleGunHudSnapshot cached;
    private static int cachedSlot = -1;

    private ParticleGunHud() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getMainHandItem().getItem() instanceof ParticleGun)) {
            cached = null;
            cachedSlot = -1;
            return;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        cached = snapshot(stack);
        cachedSlot = minecraft.player.getInventory().selected;
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || minecraft.screen != null
                || cached == null || minecraft.player.getInventory().selected != cachedSlot
                || !(minecraft.player.getMainHandItem().getItem() instanceof ParticleGun)) return;
        render(event.getGuiGraphics(), minecraft.font, cached);
    }

    private static ParticleGunHudSnapshot snapshot(ItemStack stack) {
        Magazine magazine = new Magazine();
        AttributeContainer attributes = new AttributeContainer();
        TechItemData.loadMagazine(stack, magazine);
        TechItemData.loadAttributes(stack, attributes);
        AmmoType ammo = BuiltInAmmoTypes.byId(magazine.loadedAmmoId());
        int capacity = ammo == null ? 0 : Math.max(1, (int) Math.round(ammo.reloadCapacity()
                + attributes.getValue(TechAttribute.AMMO_CAPACITY) - TechAttribute.AMMO_CAPACITY.defaultValue));
        float chargeCapacity = Math.max(1.0F, TechConfig.chargeAmmoMax());
        float heatCapacity = Math.max(0.001F, (float) attributes.getValue(TechAttribute.HEAT_CAPACITY));
        return new ParticleGunHudSnapshot(ammo, Math.max(0, magazine.charges()), capacity,
                Mth.clamp(TechItemData.getGunCharge(stack), 0.0F, chargeCapacity), chargeCapacity,
                Mth.clamp(TechItemData.getHeat(stack), 0.0F, heatCapacity), heatCapacity);
    }

    private static void render(GuiGraphics gui, Font font, ParticleGunHudSnapshot state) {
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(32, gui.guiWidth() - MARGIN * 2));
        int x = Math.max(MARGIN, gui.guiWidth() - panelWidth - MARGIN);
        int y = Math.max(MARGIN, gui.guiHeight() - HOTBAR_CLEARANCE - PANEL_HEIGHT);
        gui.fill(x, y, x + panelWidth, y + PANEL_HEIGHT, 0xDD0A0E14);
        gui.renderOutline(x, y, panelWidth, PANEL_HEIGHT, MachinePanelStyle.BORDER);
        gui.fill(x + 1, y, x + panelWidth - 1, y + 1, MachinePanelStyle.ACCENT);
        MachinePanelStyle.label(gui, font, Component.translatable("hud.transcend.particle_gun.title"), x + 7, y + 6, panelWidth - 14, MachinePanelStyle.ACCENT);
        Component magazine = state.ammo == null ? Component.translatable("hud.transcend.particle_gun.empty")
                : Component.translatable("hud.transcend.particle_gun.magazine", state.ammo.displayName(), state.charges, state.capacity);
        MachinePanelStyle.label(gui, font, magazine, x + 7, y + 19, panelWidth - 14,
                state.charges <= 0 ? 0xffe0bc80 : MachinePanelStyle.TEXT);
        drawBar(gui, font, x + 7, y + 34, panelWidth - 14, state.chargeRatio(), 0xFF00B8D4,
                Component.translatable("hud.transcend.particle_gun.charge", format(state.charge), format(state.chargeCapacity)));
        boolean overheated = state.heat >= state.heatCapacity;
        drawBar(gui, font, x + 7, y + 54, panelWidth - 14, state.heatRatio(), overheated ? 0xFFFF3D3D : 0xFFFF8A3D,
                overheated ? Component.translatable("hud.transcend.particle_gun.overheat")
                        : Component.translatable("hud.transcend.particle_gun.heat", format(state.heat), format(state.heatCapacity)));
    }

    private static void drawBar(GuiGraphics gui, Font font, int x, int y, int width, float ratio, int color, Component label) {
        MachinePanelStyle.label(gui, font, label, x, y, width, MachinePanelStyle.TEXT);
        MachinePanelStyle.bar(gui, x, y + 11, width, ratio, color);
    }

    private static String format(float value) { return String.format(java.util.Locale.ROOT, "%.0f", value); }

}
