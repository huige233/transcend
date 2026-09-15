package com.huige233.transcend.client;

import com.huige233.transcend.network.C2SReloadParticleGunAmmo;
import com.huige233.transcend.tech.ammo.AmmoType;
import com.huige233.transcend.tech.ammo.BuiltInAmmoTypes;
import com.huige233.transcend.tech.ammo.Magazine;
import com.huige233.transcend.tech.core.TechItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;


/** 展示主手粒子枪的当前弹仓并提供弹药选择，将装填请求发送给服务端处理。 */
public final class GunAmmoScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int HEIGHT = 158;

    private GunAmmoScreen() {
        super(Component.translatable("gui.transcend.gun_ammo.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GunAmmoScreen());
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        addAmmoButton(left + 12, top + 48, BuiltInAmmoTypes.charge());
        addAmmoButton(left + 12, top + 73, BuiltInAmmoTypes.byId(BuiltInAmmoTypes.ENERGY_ID));
        addAmmoButton(left + 12, top + 98, BuiltInAmmoTypes.byId(BuiltInAmmoTypes.SPELL_ID));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 70, top + 128, 100, 20).build());
    }

    private void addAmmoButton(int x, int y, AmmoType ammo) {
        if (ammo == null) return;
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.gun_ammo.load", ammo.displayName()),
                        button -> select(ammo))
                .bounds(x, y, WIDTH - 24, 20).build());
    }

    private void select(AmmoType ammo) {
        C2SReloadParticleGunAmmo.send(ammo.id());
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getMainHandItem().getItem()
                instanceof com.huige233.transcend.items.tech.ParticleGun)) {
            onClose();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        gui.fill(left, top, left + WIDTH, top + HEIGHT, 0xE00A0E14);
        gui.fill(left + 1, top + 1, left + WIDTH - 1, top + 2, 0xFF00E5FF);
        gui.drawCenteredString(font, title, width / 2, top + 12, 0xFFB388FF);

        Magazine magazine = currentMagazine();
        String current = magazine.isEmpty()
                ? Component.translatable("gui.transcend.gun_ammo.empty").getString()
                : Component.translatable("gui.transcend.gun_ammo.current", magazine.loadedAmmoId(), magazine.charges())
                        .getString();
        gui.drawCenteredString(font, current, width / 2, top + 30, 0xFFB0BEC5);
        if (!magazine.isEmpty()) {
            gui.drawCenteredString(font, Component.translatable("gui.transcend.gun_ammo.replace_warning"),
                    width / 2, top + 40, 0xFFFFAA00);
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private static Magazine currentMagazine() {
        Magazine magazine = new Magazine();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            ItemStack stack = minecraft.player.getMainHandItem();
            TechItemData.loadMagazine(stack, magazine);
        }
        return magazine;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
