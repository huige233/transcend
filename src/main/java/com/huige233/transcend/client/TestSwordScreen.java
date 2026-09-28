package com.huige233.transcend.client;

import com.huige233.transcend.items.tools.TestSword;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

   
                                
                                                     
                           
                              
   
/** 提供伤害类型搜索选择与伤害数值编辑，并直接修改客户端手持测试之剑的数据。 */
public class TestSwordScreen extends FittedScreen {
    @Override protected int minimumWidth() { return 300; }
    @Override protected int minimumHeight() { return 272; }


    
    private final ItemStack stack;
    private final InteractionHand hand;

    private EditBox typeIdBox;
    private EditBox damageBox;
    private boolean dropdownOpen = false;
    private int scroll = 0;
    private String searchFilter = "";
    private static final int VISIBLE = 6;
    private final Button[] typeEntries = new Button[VISIBLE];
    private EditBox typeSearch;

    private int panelHeight() { return dropdownOpen ? 244 : 142; }
    private int contentTop() { return (height - panelHeight()) / 2 + 28; }

    private List<DamageTypeList.DamageEntry> filteredEntries() {
        return DamageTypeList.all().stream()
                .filter(e -> searchFilter.isEmpty() || e.display().toLowerCase(Locale.ROOT).contains(searchFilter))
                .toList();
    }

    private void refreshEntries() {
        var entries = filteredEntries();
        scroll = Math.max(0, Math.min(scroll, entries.size() - VISIBLE));
        for (int i = 0; i < VISIBLE; i++) {
            Button button = typeEntries[i];
            if (button == null) continue;
            button.visible = scroll + i < entries.size();
            if (!button.visible) continue;
            String id = entries.get(scroll + i).id().toString();
            boolean selected = id.equals(currentTypeId());
            button.setMessage(Component.literal((selected ? "► " : "") + id)
                    .withStyle(selected ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(button.getMessage()));
        }
    }

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
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cx = this.width / 2;
        int top = contentTop();
        int w = 260;
        typeIdBox = null;
        typeSearch = null;

        
        addRenderableWidget(Button.builder(Component.literal("▼ ")
                                .append(Component.literal(currentTypeId()).withStyle(ChatFormatting.AQUA)),
                        b -> { dropdownOpen = !dropdownOpen; searchFilter = ""; scroll = 0; rebuild(); })
                .bounds(cx - w / 2, top, w - 8, 18).build(MachineButton::new));

        if (dropdownOpen) {
            
            typeSearch = new EditBox(this.font, cx - w / 2 + 4, top + 22, w - 60, 16,
                    Component.translatable("gui.transcend.test_sword.search"));
            typeSearch.setValue(searchFilter);
            typeSearch.setResponder(s -> { searchFilter = s.toLowerCase(Locale.ROOT); scroll = 0; refreshEntries(); });
            addRenderableWidget(typeSearch);
            setInitialFocus(typeSearch);
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> { dropdownOpen = false; rebuild(); })
                    .bounds(cx + w / 2 - 52, top + 22, 48, 16).build(MachineButton::new));

            int listTop = top + 42;
            for (int i = 0; i < VISIBLE; i++) {
                final int row = i;
                typeEntries[i] = addRenderableWidget(Button.builder(Component.empty(),
                                btn -> {
                                    var entries = filteredEntries();
                                    if (scroll + row >= entries.size()) return;
                                    TestSword.setDamageTypeId(stack, entries.get(scroll + row).id().toString());
                                    dropdownOpen = false;
                                    rebuild();
                                })
                        .bounds(cx - w / 2 + 4, listTop + i * 15, w - 8, 14).build(MachineButton::new));
            }
            refreshEntries();
        } else {
            
            typeIdBox = new EditBox(this.font, cx - w / 2 + 4, top + 22, w - 70, 16,
                    Component.translatable("gui.transcend.test_sword.custom"));
            typeIdBox.setMaxLength(128);
            typeIdBox.setValue(currentTypeId());
            addRenderableWidget(typeIdBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_sword.apply"),
                            b -> applyTypeId())
                    .bounds(cx + w / 2 - 62, top + 21, 58, 18).build(MachineButton::new));
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
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> step(-1)).bounds(bx, dmgY, 34, 18).build(MachineButton::new));
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> step(1)).bounds(bx + 38, dmgY, 34, 18).build(MachineButton::new));
        addRenderableWidget(Button.builder(Component.literal("-10"), b -> step(-10)).bounds(bx + 76, dmgY, 40, 18).build(MachineButton::new));
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> step(10)).bounds(bx + 120, dmgY, 40, 18).build(MachineButton::new));

        
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 50, dmgY + 30, 100, 20).build(MachineButton::new));
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

    @Override protected boolean clickContent(double x, double y, int button) {
        boolean wasOpen = dropdownOpen;
        boolean handled = super.clickContent(x, y, button);
        if (!wasOpen && dropdownOpen && typeSearch != null) {
            setFocused(typeSearch);
            typeSearch.setFocused(true);
        }
        return handled;
    }

    @Override
    protected void renderContent(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int cx = this.width / 2;
        int top = contentTop();
        MachinePanelStyle.frame(gui, cx - 142, top - 28, 284, panelHeight(), 0xffc59750);
        MachinePanelStyle.label(gui, font, title, cx - 130, top - 20, 260, MachinePanelStyle.TEXT);
        if (dropdownOpen && filteredEntries().isEmpty()) {
            gui.drawCenteredString(font, Component.translatable("gui.transcend.ui.no_results"), cx, top + 76, MachinePanelStyle.MUTED);
        }

        
        gui.drawString(this.font, tr("damage"), cx - w2() + 4,
                (dropdownOpen ? top + 150 : top + 52) - 9, 0xFF9FB0C0);

        super.renderContent(gui, mouseX, mouseY, partialTick);
    }

    private static int w2() {
        return 130;
    }

    private static String tr(String key) {
        return net.minecraft.client.resources.language.I18n.get("gui.transcend.test_sword." + key);
    }

    @Override
    protected boolean scrollContent(double mouseX, double mouseY, double delta) {
        if (dropdownOpen && mouseX >= width / 2 - 130 && mouseX < width / 2 + 130
                && mouseY >= contentTop() + 42 && mouseY < contentTop() + 132 && delta != 0) {
            int maxScroll = Math.max(0, filteredEntries().size() - VISIBLE);
            scroll = delta < 0 ? Math.min(maxScroll, scroll + 1) : Math.max(0, scroll - 1);
            refreshEntries();
            return true;
        }
        return super.scrollContent(mouseX, mouseY, delta);
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
