package com.huige233.transcend.client;

import com.huige233.transcend.entity.TestDummy;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.C2STestDummySettingsPack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** 木桩测试 GUI 界面。 */
public class TestDummyScreen extends Screen {

    private final int entityId;
    private int armorValue = 0;
    private int resistValue = 0;

    public TestDummyScreen(TestDummy dummy) {
        super(Component.translatable("gui.transcend.test_dummy.title"));
        this.entityId = dummy.getId();
        this.armorValue = (int) dummy.getArmorValue();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int bw = 160;
        int bh = 20;
        int startY = cy - 60;

        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.clear"), b -> send(0, 0))
                .bounds(cx - bw / 2, startY, bw, bh).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.toggle_announce"), b -> send(1, 0))
                .bounds(cx - bw / 2, startY + 26, bw, bh).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.armor_down"), b -> {
            armorValue = Math.max(0, armorValue - 5);
            send(2, armorValue);
        }).bounds(cx - bw / 2, startY + 52, bw / 2 - 2, bh).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.armor_up"), b -> {
            armorValue = Math.min(30, armorValue + 5);
            send(2, armorValue);
        }).bounds(cx + 2, startY + 52, bw / 2 - 2, bh).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.resistance_down"), b -> {
            resistValue = Math.max(0, resistValue - 1);
            send(3, resistValue);
        }).bounds(cx - bw / 2, startY + 78, bw / 2 - 2, bh).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.resistance_up"), b -> {
            resistValue = Math.min(4, resistValue + 1);
            send(3, resistValue);
        }).bounds(cx + 2, startY + 78, bw / 2 - 2, bh).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.remove"), b -> {
            send(4, 0);
            onClose();
        }).bounds(cx - bw / 2, startY + 110, bw, bh).build());
    }

    private void send(int action, int value) {
        NetworkHandler.CHANNEL.sendToServer(new C2STestDummySettingsPack(entityId, action, value));
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int cx = this.width / 2;
        int cy = this.height / 2;

        gui.fill(cx - 100, cy - 80, cx + 100, cy + 80, 0xCC222222);
        gui.fill(cx - 99, cy - 79, cx + 99, cy + 79, 0xCC111111);

        gui.drawCenteredString(this.font, title, cx, cy - 74, 0xFFFFFF);

        gui.drawString(this.font, Component.translatable("gui.transcend.test_dummy.armor", armorValue), cx - 90, cy - 6, 0xAAAAAA);
        gui.drawString(this.font, Component.translatable("gui.transcend.test_dummy.resistance", resistValue), cx - 90, cy + 20, 0xAAAAAA);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
