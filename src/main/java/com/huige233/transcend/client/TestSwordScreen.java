package com.huige233.transcend.client;

import com.huige233.transcend.items.tools.TestSword;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

   
                                
                                                     
                           
                              
   
/** 提供伤害类型搜索选择与伤害数值编辑，并直接修改客户端手持测试之剑的数据。 */
public class TestSwordScreen extends Screen {

    
    private final ItemStack stack;
    private final InteractionHand hand;

    private EditBox typeIdBox;
    private EditBox damageBox;
    private boolean dropdownOpen = false;
    private int scroll = 0;
    private String searchFilter = "";
    private static final int VISIBLE = 6;

    public static void open(ItemStack stack, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new TestSwordScreen(stack, hand));
    }

    private TestSwordScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("gui.transcend.test_sword.title"));
        this.stack = stack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cx = this.width / 2;
        int top = this.height / 2 - 80;
        int w = 260;

        
        addRenderableWidget(Button.builder(Component.literal("▼ ")
                                .append(Component.literal(currentTypeId()).withStyle(ChatFormatting.AQUA)),
                        b -> { dropdownOpen = !dropdownOpen; searchFilter = ""; rebuild(); })
                .bounds(cx - w / 2, top, w - 8, 18).build());

        if (dropdownOpen) {
            
            var search = new EditBox(this.font, cx - w / 2 + 4, top + 22, w - 60, 16,
                    Component.translatable("gui.transcend.test_sword.search"));
            search.setValue(searchFilter);
            search.setResponder(s -> { searchFilter = s.toLowerCase(Locale.ROOT); scroll = 0; rebuild(); });
            addRenderableWidget(search);
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> { dropdownOpen = false; rebuild(); })
                    .bounds(cx + w / 2 - 52, top + 22, 48, 16).build());

            List<DamageTypeList.DamageEntry> entries = DamageTypeList.all().stream()
                    .filter(e -> searchFilter.isEmpty() || e.display().contains(searchFilter))
                    .toList();
            int listTop = top + 42;
            for (int i = 0; i < VISIBLE && scroll + i < entries.size(); i++) {
                DamageTypeList.DamageEntry e = entries.get(scroll + i);
                String id = e.id().toString();
                boolean sel = id.equals(currentTypeId());
                addRenderableWidget(Button.builder(
                                Component.literal((sel ? "► " : "") + id)
                                        .withStyle(sel ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                                btn -> {
                                    TestSword.setDamageTypeId(stack, id);
                                    dropdownOpen = false;
                                    rebuild();
                                })
                        .bounds(cx - w / 2 + 4, listTop + i * 15, w - 8, 14).build());
            }
        } else {
            
            typeIdBox = new EditBox(this.font, cx - w / 2 + 4, top + 22, w - 70, 16,
                    Component.translatable("gui.transcend.test_sword.custom"));
            typeIdBox.setMaxLength(128);
            typeIdBox.setValue(currentTypeId());
            addRenderableWidget(typeIdBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_sword.apply"),
                            b -> applyTypeId())
                    .bounds(cx + w / 2 - 62, top + 21, 58, 18).build());
        }

        
        int dmgY = dropdownOpen ? top + 150 : top + 52;
        damageBox = new EditBox(this.font, cx - w / 2 + 4, dmgY, 90, 18,
                Component.translatable("gui.transcend.test_sword.damage"));
        damageBox.setMaxLength(10);
        damageBox.setValue(String.valueOf((int) TestSword.getConfiguredDamage(stack)));
        damageBox.setResponder(s -> {
            try { TestSword.setDamage(stack, Float.parseFloat(s.trim())); } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(damageBox);

        int bx = cx - w / 2 + 100;
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> step(-1)).bounds(bx, dmgY, 34, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> step(1)).bounds(bx + 38, dmgY, 34, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-10"), b -> step(-10)).bounds(bx + 76, dmgY, 40, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> step(10)).bounds(bx + 120, dmgY, 40, 18).build());

        
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 50, dmgY + 30, 100, 20).build());
    }

    private void applyTypeId() {
        if (typeIdBox != null && !typeIdBox.getValue().isBlank()) {
            TestSword.setDamageTypeId(stack, typeIdBox.getValue().trim());
        }
    }

    private void step(int delta) {
        try {
            float v = damageBox != null ? Float.parseFloat(damageBox.getValue().trim())
                    : TestSword.getConfiguredDamage(stack);
            TestSword.setDamage(stack, Math.max(0, v + delta));
        } catch (NumberFormatException ignored) {
        }
        refreshBox();
    }

    private void refreshBox() {
        if (damageBox != null) {
            damageBox.setValue(String.valueOf((int) TestSword.getConfiguredDamage(stack)));
        }
    }

    private String currentTypeId() {
        return TestSword.getDamageTypeId(stack);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int cx = this.width / 2;
        int top = this.height / 2 - 80;
        int panelH = dropdownOpen ? 250 : 130;
        gui.fill(cx - 134, top - 14, cx + 134, top + panelH, 0xE0101018);
        gui.fill(cx - 132, top - 12, cx + 132, top + panelH - 2, 0xF01C1C28);
        gui.drawCenteredString(this.font, title, cx, top - 9, 0xFFFFFF);

        
        gui.drawString(this.font, tr("damage"), cx - w2() + 4,
                (dropdownOpen ? top + 150 : top + 52) - 9, 0xFF9FB0C0);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private static int w2() {
        return 130;
    }

    private static String tr(String key) {
        return net.minecraft.client.resources.language.I18n.get("gui.transcend.test_sword." + key);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (dropdownOpen) {
            List<DamageTypeList.DamageEntry> entries = DamageTypeList.all();
            long filtered = searchFilter.isEmpty() ? entries.size()
                    : entries.stream().filter(e -> e.display().contains(searchFilter)).count();
            int maxScroll = Math.max(0, (int) filtered - VISIBLE);
            scroll = delta < 0 ? Math.min(maxScroll, scroll + 1) : Math.max(0, scroll - 1);
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean boxFocused = (typeIdBox != null && typeIdBox.isFocused())
                || (damageBox != null && damageBox.isFocused());
        if (boxFocused) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
