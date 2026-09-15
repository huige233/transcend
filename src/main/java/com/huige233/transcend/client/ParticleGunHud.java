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
    private static final int PANEL_HEIGHT = 68;
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
        render(event.getGuiGraphics(), minecraft.font, snapshot(minecraft.player.getMainHandItem()));
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
        int x = gui.guiWidth() - PANEL_WIDTH - MARGIN;
        int y = gui.guiHeight() - HOTBAR_CLEARANCE - PANEL_HEIGHT;
        gui.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xDD0A0E14);
        gui.fill(x, y, x + PANEL_WIDTH, y + 1, 0xFF00E5FF);
        gui.fill(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF2A3242);
        gui.drawString(font, Component.translatable("hud.transcend.particle_gun.title"), x + 7, y + 6, 0xFFB388FF, false);
        Component magazine = state.ammo == null ? Component.translatable("hud.transcend.particle_gun.empty")
                : Component.translatable("hud.transcend.particle_gun.magazine", state.ammo.displayName(), state.charges, state.capacity);
        gui.drawString(font, magazine, x + 7, y + 19, 0xFFE0E6ED, false);
        drawBar(gui, font, x + 7, y + 34, PANEL_WIDTH - 14, state.chargeRatio(), 0xFF00B8D4,
                Component.translatable("hud.transcend.particle_gun.charge", format(state.charge), format(state.chargeCapacity)));
        boolean overheated = state.heat >= state.heatCapacity;
        drawBar(gui, font, x + 7, y + 49, PANEL_WIDTH - 14, state.heatRatio(), overheated ? 0xFFFF3D3D : 0xFFFF8A3D,
                overheated ? Component.translatable("hud.transcend.particle_gun.overheat")
                        : Component.translatable("hud.transcend.particle_gun.heat", format(state.heat), format(state.heatCapacity)));
    }

    private static void drawBar(GuiGraphics gui, Font font, int x, int y, int width, float ratio, int color, Component label) {
        gui.fill(x, y, x + width, y + 9, 0xFF202938);
        int filled = Math.round(width * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) gui.fill(x, y, x + filled, y + 9, color);
        gui.drawString(font, label, x + 3, y + 1, 0xFFFFFFFF, true);
    }

    private static String format(float value) { return String.format(java.util.Locale.ROOT, "%.0f", value); }

}
