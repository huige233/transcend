package com.huige233.transcend.client;

import com.huige233.transcend.network.C2SReloadParticleGunAmmo;
import com.huige233.transcend.tech.ammo.AmmoType;
import com.huige233.transcend.tech.ammo.BuiltInAmmoTypes;
import com.huige233.transcend.tech.ammo.Magazine;
import com.huige233.transcend.tech.core.TechItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;


/** 展示主手粒子枪的当前弹仓并提供弹药选择，将装填请求发送给服务端处理。 */
public final class GunAmmoScreen extends FittedScreen {
    @Override protected int minimumWidth() { return WIDTH + 16; }
    @Override protected int minimumHeight() { return HEIGHT + 16; }

    private static final int WIDTH = 280;
    private static final int HEIGHT = 188;

    private GunAmmoScreen() {
        super(Component.translatable("gui.transcend.gun_ammo.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GunAmmoScreen());
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        addAmmoButton(left + 12, top + 72, BuiltInAmmoTypes.charge());
        addAmmoButton(left + 12, top + 96, BuiltInAmmoTypes.byId(BuiltInAmmoTypes.ENERGY_ID));
        addAmmoButton(left + 12, top + 120, BuiltInAmmoTypes.byId(BuiltInAmmoTypes.SPELL_ID));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + (WIDTH - 100) / 2, top + 156, 100, 20).build(MachineButton::new));
    }

    private void addAmmoButton(int x, int y, AmmoType ammo) {
        if (ammo == null) return;
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.gun_ammo.load", ammo.displayName()),
                        button -> select(ammo))
                .bounds(x, y, WIDTH - 24, 20).build(MachineButton::new));
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
    protected void renderContent(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        MachinePanelStyle.frame(gui, left, top, WIDTH, HEIGHT, MachinePanelStyle.ACCENT);
        MachinePanelStyle.label(gui, font, title, left + 12, top + 8, WIDTH - 24, MachinePanelStyle.TEXT);

        Magazine magazine = currentMagazine();
        MachinePanelStyle.label(gui, font, magazineLabel(magazine), left + 12, top + 30, WIDTH - 24, MachinePanelStyle.TEXT);
        if (!magazine.isEmpty()) {
            int lineY = top + 44;
            for (var line : font.split(Component.translatable("gui.transcend.gun_ammo.replace_warning"), WIDTH - 24)) {
                gui.drawString(font, line, left + 12, lineY, 0xffe0bc80, false);
                lineY += font.lineHeight;
            }
        }
        super.renderContent(gui, mouseX, mouseY, partialTick);
    }

    private static Component magazineLabel(Magazine magazine) {
        if (magazine.isEmpty()) return Component.translatable("gui.transcend.gun_ammo.empty");
        AmmoType ammo = BuiltInAmmoTypes.byId(magazine.loadedAmmoId());
        return Component.translatable("gui.transcend.gun_ammo.current",
                ammo == null ? magazine.loadedAmmoId() : ammo.displayName(), magazine.charges());
    }

    @Override protected void renderHints(GuiGraphics gui, int mouseX, int mouseY, int tooltipX, int tooltipY) {
        int left = (width - WIDTH) / 2, top = (height - HEIGHT) / 2;
        if (mouseX >= left + 12 && mouseX < left + WIDTH - 12 && mouseY >= top + 30 && mouseY < top + 40)
            gui.renderTooltip(font, font.split(magazineLabel(currentMagazine()), Math.min(260, gui.guiWidth() - 16)), tooltipX, tooltipY);
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
