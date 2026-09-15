package com.huige233.transcend.client;

import com.huige233.transcend.entity.TestDummy;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.C2STestDummySettingsPack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

   
                     
                                  
                                                  
                                           
                                   
   
/** 通过统计、设置、护盾和增益四页查看测试假人数据，并向服务端提交配置、清除或移除请求。 */
public class TestDummyScreen extends Screen {

    private static final int PAGE_STATS = 0;
    private static final int PAGE_SETTINGS = 1;
    private static final int PAGE_SHIELD = 2;
    private static final int PAGE_BUFFS = 3;
    private static final int PAGE_COUNT = 4;

    private final int entityId;
    private final TestDummy dummy;
    private int page = PAGE_STATS;

    
    private static final int PANEL_W = 276;
    private static final int PANEL_H = 360;
    private static final int TAB_H = 20;
    
    private static final int CONTENT_Y = TAB_H + 8;

    private static final int ROW_H = 22;
    private static final int BTN_H = 18;

    private static final int SW_NO_KNOCKBACK = 0;
    private static final int SW_HIT_EFFECT = 1;
    private static final int SW_REDUCE = 2;
    private static final int SW_REGEN_ON_HIT = 3;
    private static final int SW_SHIELD_RESIST = 4;
    private static final int SW_ANNOUNCE = 5;

    
    private int contentW() { return PANEL_W - 12; }

    
    private int resistValue = 0;
    private boolean noKnockback = false;
    
    private int kindIndex = 0;
    
    private int shieldMax = 0;
    
    private int shieldRegenStep = 2;
    
    private boolean regenOnHit = false;
    
    private int shieldRegenDelay = 60;
    
    private int regenOnHitPct = 5;
    
    private int healModeIndex = 0;
    
    private boolean shieldResist = true;
    
    private boolean reduceAnnounce = true;
    
    private boolean shieldAnnounce = true;
    private boolean hitEffect = true;
    private final int[] categoryResist = new int[TestDummy.DamageCategory.values().length];
    
    private final int[] shieldToughness = new int[TestDummy.DamageCategory.values().length];
    
    private final java.util.Map<Integer, Supplier<Component>> switchLabelSuppliers = new java.util.HashMap<>();

    
    private EditBox buffLevelBox;
    private boolean buffDropdownOpen = false;
    private int buffSelected = 0;
    private int buffScroll = 0;
    private int buffLevel = 0;
    private static final int BUFF_VISIBLE = 5;
    private static final int BUFF_ACTIVE_VISIBLE = 8;

    
    private float sLast, sMax, sTotal, sDps, sBurst, sRaw;
    private int sHits;

    public TestDummyScreen(TestDummy dummy) {
        super(Component.translatable("gui.transcend.test_dummy.title"));
        this.dummy = dummy;
        this.entityId = dummy.getId();
        refreshMirrors();
    }

    private void refreshMirrors() {
        
        this.activeBuffs = dummy.getActiveBuffDescriptions();
        this.resistValue = dummy.getResistanceLevel();
        this.reduceAnnounce = dummy.isAnnounceReduce();
        this.shieldAnnounce = dummy.isAnnounceReduce();
        this.kindIndex = dummy.getKind().ordinal();
        this.shieldMax = (int) dummy.getShieldMax();
        this.shieldRegenStep = dummy.getShieldRegenStep();
        this.regenOnHit = dummy.isShieldRegenOnHit();
        this.shieldRegenDelay = dummy.getShieldRegenDelay();
        this.regenOnHitPct = dummy.getShieldRegenOnHitPercent();
        this.healModeIndex = dummy.getHealMode().index;
        this.shieldResist = dummy.isShieldAffectedByResist();
        this.noKnockback = dummy.isNoKnockback();
        this.hitEffect = dummy.isHitEffectEnabled();
        for (TestDummy.DamageCategory cat : TestDummy.DamageCategory.values()) {
            categoryResist[cat.index] = dummy.getCategoryResistance(cat.index);
            shieldToughness[cat.index] = dummy.getShieldToughness(cat.index);
        }
    }

    @Override
    protected void init() {
        layoutPage();
    }

    private int panelLeft() {
        return this.width / 2 - PANEL_W / 2;
    }

    private int panelTop() {
        return Math.max(20, this.height / 2 - PANEL_H / 2);
    }

    private void layoutPage() {
        clearWidgets();
        int px = panelLeft();
        int py = panelTop();

        
        int tabW = (PANEL_W - 12) / 4;
        addRenderableWidget(tab("tab_stats", PAGE_STATS, px + 4, py + 4, tabW));
        addRenderableWidget(tab("tab_settings", PAGE_SETTINGS, px + 5 + tabW, py + 4, tabW));
        addRenderableWidget(tab("tab_shield", PAGE_SHIELD, px + 6 + tabW * 2, py + 4, tabW));
        addRenderableWidget(tab("tab_buffs", PAGE_BUFFS, px + 8 + tabW * 3, py + 4, tabW));

        int cy0 = py + CONTENT_Y;

        switchLabelSuppliers.clear();
        if (page == PAGE_STATS) {
            
            int bottomY = py + PANEL_H - 26;
            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.clear"), b -> send(0, 0))
                    .bounds(px + PANEL_W / 2 - 122, bottomY, 118, BTN_H).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.remove")
                            .copy().withStyle(ChatFormatting.RED), b -> { send(4, 0); onClose(); })
                    .bounds(px + PANEL_W / 2 + 4, bottomY, 118, BTN_H).build());
        } else if (page == PAGE_SETTINGS) {
            
            int row0 = cy0 + 8;
            
            int rowY = row0;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                        int step = isShiftDown() ? 10 : 1;
                        resistValue = Math.max(0, resistValue - step);
                        send(3, resistValue);
                    }).bounds(px + 150, rowY, 22, BTN_H).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                        int step = isShiftDown() ? 10 : 1;
                        resistValue = Math.min(100, resistValue + step);
                        send(3, resistValue);
                    }).bounds(px + 176, rowY, 22, BTN_H).build());

            
            
            int resistBaseY = rowY + ROW_H + 8;
            for (TestDummy.DamageCategory cat : TestDummy.DamageCategory.values()) {
                int col = cat.index % 2;
                int row = cat.index / 2;
                int bx = px + 6 + col * 130;
                int by = resistBaseY + row * ROW_H;
                int idx = cat.index;
                addRenderableWidget(Button.builder(Component.literal("−"), b -> {
                            int step = isShiftDown() ? 10 : 1;
                            categoryResist[idx] = Math.max(0, categoryResist[idx] - step);
                            send(7, encodeResist(idx, categoryResist[idx]));
                        }).bounds(bx, by, 18, BTN_H).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                            int step = isShiftDown() ? 10 : 1;
                            categoryResist[idx] = Math.min(100, categoryResist[idx] + step);
                            send(7, encodeResist(idx, categoryResist[idx]));
                        }).bounds(bx + 106, by, 18, BTN_H).build());
            }

            
            int kindY = resistBaseY + 3 * ROW_H + 8;
            addRenderableWidget(Button.builder(kindLabel(), b -> {
                        kindIndex = (kindIndex + 1) % TestDummy.DummyKind.values().length;
                        send(12, kindIndex);
                        b.setMessage(kindLabel());
                    }).bounds(px + 152, kindY, 112, BTN_H).build());

            
            int healY = kindY + ROW_H + 4;
            addRenderableWidget(Button.builder(healModeLabel(), b -> {
                        healModeIndex = (healModeIndex + 1) % TestDummy.HealMode.values().length;
                        send(21, healModeIndex);
                        b.setMessage(healModeLabel());
                    }).bounds(px + 152, healY, 112, BTN_H).build());
        } else if (page == PAGE_SHIELD) {
            
            int row0 = cy0 + 8;
            
            int shieldY = row0;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                        int step = isCtrlDown() ? 1000 : isShiftDown() ? 100 : 10;
                        shieldMax = Math.max(0, shieldMax - step);
                        send(14, shieldMax);
                    }).bounds(px + 152, shieldY, 22, BTN_H).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                        int step = isCtrlDown() ? 1000 : isShiftDown() ? 100 : 10;
                        shieldMax += step;
                        send(14, shieldMax);
                    }).bounds(px + 178, shieldY, 22, BTN_H).build());

            
            int regenY = shieldY + ROW_H;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> { shieldRegenStep = Math.max(1, shieldRegenStep - 1); send(15, shieldRegenStep); })
                    .bounds(px + 152, regenY, 22, BTN_H).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> { shieldRegenStep = Math.min(20, shieldRegenStep + 1); send(15, shieldRegenStep); })
                    .bounds(px + 178, regenY, 22, BTN_H).build());

            
            int delayY = regenY + ROW_H;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                        int step = isShiftDown() ? 60 : 10;
                        shieldRegenDelay = Math.max(0, shieldRegenDelay - step);
                        send(19, shieldRegenDelay);
                    }).bounds(px + 152, delayY, 22, BTN_H).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                        int step = isShiftDown() ? 60 : 10;
                        shieldRegenDelay = Math.min(600, shieldRegenDelay + step);
                        send(19, shieldRegenDelay);
                    }).bounds(px + 178, delayY, 22, BTN_H).build());

            
            int onHitPctY = delayY + ROW_H;
            addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                        int step = isShiftDown() ? 10 : 1;
                        regenOnHitPct = Math.max(0, regenOnHitPct - step);
                        send(20, regenOnHitPct);
                    }).bounds(px + 152, onHitPctY, 22, BTN_H).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                        int step = isShiftDown() ? 10 : 1;
                        regenOnHitPct = Math.min(100, regenOnHitPct + step);
                        send(20, regenOnHitPct);
                    }).bounds(px + 178, onHitPctY, 22, BTN_H).build());

            
            
            int toughBaseY = onHitPctY + ROW_H + 4;
            for (TestDummy.DamageCategory cat : TestDummy.DamageCategory.values()) {
                int col = cat.index % 2;
                int row = cat.index / 2;
                int bx = px + 6 + col * 130;
                int by = toughBaseY + row * ROW_H;
                int idx = cat.index;
                addRenderableWidget(Button.builder(Component.literal("−"), b -> {
                            int step = isShiftDown() ? 5 : 1;
                            shieldToughness[idx] = Math.max(1, shieldToughness[idx] - step);
                            send(18, encodeResist(idx, shieldToughness[idx]));
                        }).bounds(bx, by, 18, BTN_H).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                            int step = isShiftDown() ? 5 : 1;
                            shieldToughness[idx] = Math.min(100, shieldToughness[idx] + step);
                            send(18, encodeResist(idx, shieldToughness[idx]));
                        }).bounds(bx + 106, by, 18, BTN_H).build());
            }

            
            int shieldSwY = toughBaseY + 3 * ROW_H + 12;
            addRenderableWidget(switchBtn(SW_REGEN_ON_HIT, this::regenOnHitLabel, () -> regenOnHit = !regenOnHit,
                    () -> send(16, regenOnHit ? 1 : 0), px + 6, shieldSwY, 128));
            addRenderableWidget(switchBtn(SW_SHIELD_RESIST, this::shieldResistLabel, () -> shieldResist = !shieldResist,
                    () -> send(17, shieldResist ? 1 : 0), px + 138, shieldSwY, 128));

            
            int swY = shieldSwY + ROW_H + 6;
            addRenderableWidget(switchBtn(SW_ANNOUNCE, this::shieldAnnounceLabel, () -> shieldAnnounce = !shieldAnnounce,
                    () -> send(13, shieldAnnounce ? 1 : 0), px + 6, swY, 128));
        } else {
            
            int boxTop = cy0 + 6;
            addRenderableWidget(Button.builder(Component.literal("▼ ").append(selectedBuffDisplay()), b -> {
                        buffDropdownOpen = !buffDropdownOpen;
                        layoutPage();
                    }).bounds(px + 6, boxTop, PANEL_W - 12, 18).build());

            buffLevelBox = new EditBox(this.font, px + 6, boxTop + 22, 36, 18,
                    Component.translatable("gui.transcend.test_dummy.buff_level"));
            buffLevelBox.setMaxLength(3);
            buffLevelBox.setValue(String.valueOf(buffLevel));
            buffLevelBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
            buffLevelBox.setResponder(s -> {
                try { buffLevel = Math.max(0, Math.min(255, Integer.parseInt(s))); } catch (NumberFormatException ignored) {}
            });
            addRenderableWidget(buffLevelBox);

            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.buff_add"), b ->
                            sendStr(8, selectedBuffId(), buffLevel))
                    .bounds(px + 46, boxTop + 22, 62, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.buff_remove"), b ->
                            sendStr(9, selectedBuffId(), 0))
                    .bounds(px + 112, boxTop + 22, 62, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.transcend.test_dummy.buff_clear"), b ->
                            send(10, 0))
                    .bounds(px + 178, boxTop + 22, 76, 18).build());

            if (buffDropdownOpen) {
                List<BuffRegistryList.BuffEntry> entries = BuffRegistryList.all();
                buffScroll = Mth_clamp(buffScroll, 0, Math.max(0, entries.size() - BUFF_VISIBLE));
                int listTop = boxTop + 44;
                for (int i = 0; i < BUFF_VISIBLE && buffScroll + i < entries.size(); i++) {
                    BuffRegistryList.BuffEntry e = entries.get(buffScroll + i);
                    int idx = buffScroll + i;
                    boolean sel = idx == buffSelected;
                    addRenderableWidget(Button.builder(
                                    Component.literal((sel ? "► " : "") + e.display()),
                                    b -> { buffSelected = idx; buffDropdownOpen = false; layoutPage(); })
                            .bounds(px + 6, listTop + i * 15, PANEL_W - 12, 14).build());
                }
            } else {
                
                int listTop = cy0 + 62;
                int maxShow = 7;
                activeBuffScroll = Mth_clamp(activeBuffScroll, 0, Math.max(0, activeBuffs.size() - maxShow));
                for (int row = 0; row < Math.min(activeBuffs.size() - activeBuffScroll, maxShow); row++) {
                    int i = activeBuffScroll + row;
                    String raw = activeBuffs.get(i);
                    final String effectId = extractBuffId(raw);
                    addRenderableWidget(Button.builder(
                                    Component.literal("✦ ").withStyle(ChatFormatting.GREEN)
                                            .append(ellipsize(raw, PANEL_W - 26)),
                                    b -> selectBuffById(effectId))
                            .bounds(px + 6, listTop + row * 16, PANEL_W - 12, 15).build());
                }
            }
        }
    }

    
    private static String extractBuffId(String desc) {
        int sp = desc.lastIndexOf(' ');
        return sp > 0 ? desc.substring(0, sp) : desc;
    }

    
    private void selectBuffById(String effectId) {
        List<BuffRegistryList.BuffEntry> entries = BuffRegistryList.all();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().toString().equals(effectId)) {
                buffSelected = i;
                break;
            }
        }
    }

    
    private int activeBuffScroll = 0;

    
    private Button switchBtn(int id, Supplier<Component> labelSup, Runnable flip, Runnable packet, int x, int y, int w) {
        switchLabelSuppliers.put(id, labelSup);
        return Button.builder(labelSup.get(), b -> {
            flip.run();
            packet.run();
            Supplier<Component> sup = switchLabelSuppliers.get(id);
            b.setMessage(sup != null ? sup.get() : labelSup.get());
        }).bounds(x, y, w, BTN_H).build();
    }

    private static String I18n_get(String key) {
        return net.minecraft.client.resources.language.I18n.get("gui.transcend.test_dummy." + key);
    }

    private static int Mth_clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private Button tab(String key, int targetPage, int x, int y, int w) {
        return Button.builder(Component.translatable("gui.transcend.test_dummy." + key), b -> {
            page = targetPage;
            layoutPage();
        }).bounds(x, y, w, TAB_H).build();
    }

    private Component kindLabel() {
        String[] keys = {"kind_normal", "kind_undead", "kind_arthropod", "kind_illager", "kind_water"};
        return Component.translatable("gui.transcend.test_dummy.kind",
                Component.translatable("gui.transcend.test_dummy." + keys[Math.min(kindIndex, keys.length - 1)]));
    }

    
    private Component healModeLabel() {
        String[] keys = {"heal_immediate", "heal_interval", "heal_low_hp", "heal_none"};
        return Component.translatable("gui.transcend.test_dummy.heal_mode",
                Component.translatable("gui.transcend.test_dummy." + keys[Math.min(healModeIndex, keys.length - 1)]));
    }


    private Component selectedBuffDisplay() {
        List<BuffRegistryList.BuffEntry> entries = BuffRegistryList.all();
        if (entries.isEmpty()) return Component.literal("minecraft:speed");
        BuffRegistryList.BuffEntry e = entries.get(Math.min(buffSelected, entries.size() - 1));
        return Component.literal(e.localizedName()).append(
                Component.literal(" (" + e.id() + ")").withStyle(ChatFormatting.GRAY));
    }

    private String selectedBuffId() {
        List<BuffRegistryList.BuffEntry> entries = BuffRegistryList.all();
        if (entries.isEmpty()) return "minecraft:speed";
        return entries.get(Math.min(buffSelected, entries.size() - 1)).id().toString();
    }

    private Component noKnockbackLabel() {
        return Component.translatable("gui.transcend.test_dummy.no_knockback",
                Component.translatable(noKnockback ? "gui.transcend.test_dummy.on" : "gui.transcend.test_dummy.off"));
    }

    private Component hitEffectLabel() {
        return Component.translatable("gui.transcend.test_dummy.hit_effect",
                Component.translatable(hitEffect ? "gui.transcend.test_dummy.on" : "gui.transcend.test_dummy.off"));
    }

    private Component reduceLabel() {
        return Component.translatable("gui.transcend.test_dummy.reduce_announce",
                Component.translatable(reduceAnnounce ? "gui.transcend.test_dummy.on" : "gui.transcend.test_dummy.off"));
    }

    private Component regenOnHitLabel() {
        return Component.translatable("gui.transcend.test_dummy.regen_on_hit")
                .append(" ")
                .append(Component.translatable(regenOnHit ? "gui.transcend.test_dummy.on" : "gui.transcend.test_dummy.off"))
                .withStyle(regenOnHit ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private Component shieldResistLabel() {
        return Component.translatable("gui.transcend.test_dummy.shield_resist")
                .append(" ")
                .append(Component.translatable(shieldResist ? "gui.transcend.test_dummy.on" : "gui.transcend.test_dummy.off"))
                .withStyle(shieldResist ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private Component shieldAnnounceLabel() {
        return Component.translatable("gui.transcend.test_dummy.reduce_announce")
                .append(" ")
                .append(Component.translatable(shieldAnnounce ? "gui.transcend.test_dummy.on" : "gui.transcend.test_dummy.off"))
                .withStyle(shieldAnnounce ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static int encodeResist(int categoryIndex, int percent) {
        return (categoryIndex << 16) | (percent & 0xFFFF);
    }

       
                                                               
                      
       
    private static int resistTierColor(int pct) {
        if (pct >= 100) return 0xFFFF5555;        
        if (pct >= 75)  return 0xFFCC66FF;        
        if (pct >= 50)  return 0xFF55CCFF;       
        if (pct >= 30)  return 0xFF55FF55;       
        if (pct > 0)    return 0xFFFFFF55;       
        return 0xFFF0F0F0;                       
    }

    private void send(int action, int value) {
        NetworkHandler.CHANNEL.sendToServer(new C2STestDummySettingsPack(entityId, action, value));
    }

    private void sendStr(int action, String str, int value) {
        NetworkHandler.CHANNEL.sendToServer(new C2STestDummySettingsPack(entityId, action, value, str));
    }

    @Override
    public void tick() {
        super.tick();
        if (dummy.isRemoved()) {
            onClose();
            return;
        }
        sLast = dummy.getLastDamage();
        sRaw = dummy.getRawDamage();
        sMax = dummy.getMaxHit();
        sTotal = dummy.getTotalDamage();
        sDps = dummy.getDps();
        sBurst = dummy.getBurstDamage();
        sHits = dummy.getHitCount();
        activeBuffs = dummy.getActiveBuffDescriptions();
        
        kindIndex = dummy.getKind().ordinal();
        healModeIndex = dummy.getHealMode().index;
    }

    private List<String> activeBuffs = List.of();

    
    private boolean isShiftDown() {
        return this.hasShiftDown();
    }

    
    private boolean isCtrlDown() {
        return this.hasControlDown();
    }

    private static String tr(String key) {
        return I18n_get(key);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int px = panelLeft();
        int py = panelTop();

        
        gui.fill(px - 3, py - 3, px + PANEL_W + 3, py + PANEL_H + 3, 0xE0101018);
        gui.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF01C1C28);
        gui.fill(px, py, px + PANEL_W, py + 1, 0xFFCC8844);
        gui.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, 0xCC444455);

        gui.drawCenteredString(this.font, title, px + PANEL_W / 2, py - 14, 0xFFFFFF);

        int cy0 = py + CONTENT_Y;

        if (page == PAGE_STATS) {
            
            int cardY = cy0 + 10;
            statCard(gui, px + 10, cardY, tr("last_hit"), fmt(sLast),
                    sRaw > 0 && Math.abs(sRaw - sLast) > 0.5F ? fmtRaw(sRaw) : null);
            statCard(gui, px + 132, cardY, tr("max_hit"), fmt(sMax), null);
            statCard(gui, px + 10, cardY + 44, tr("dps"), fmt(sDps), null);
            statCard(gui, px + 132, cardY + 44, tr("burst"), fmt(sBurst), null);
            statCard(gui, px + 10, cardY + 88, tr("hits"), String.valueOf(sHits), null);
            statCard(gui, px + 132, cardY + 88, tr("total"), fmt(sTotal), null);
        } else if (page == PAGE_SETTINGS) {
            
            int row0 = cy0 + 8;
            
            String resistText = resistValue * 20 + "%";
            int rColor = resistValue >= 4 ? 0xFFFF5555 : resistValue > 0 ? 0xFFFFFFA0 : 0xFF667788;
            gui.drawString(this.font, tr("resistance"), px + 10, row0 + 5, 0xFF9FB0C0);
            gui.drawString(this.font, resistText, px + 146 - this.font.width(resistText), row0 + 5, rColor, true);

            
            int resistBaseY = row0 + ROW_H + 8;
            for (TestDummy.DamageCategory cat : TestDummy.DamageCategory.values()) {
                int col = cat.index % 2;
                int row = cat.index / 2;
                int bx = px + 6 + col * 130;
                int by = resistBaseY + row * ROW_H;
                String name = tr("cat_" + cat.name().toLowerCase(Locale.ROOT));
                int pct = categoryResist[cat.index];
                String pctText = pct + "%";
                int tierColor = resistTierColor(pct);
                int nameColor = pct >= 100 ? tierColor : 0xFFCCCCCC;
                gui.drawString(this.font, name, bx + 22, by + 5, nameColor);
                gui.drawString(this.font, pctText, bx + 104 - this.font.width(pctText), by + 5,
                        tierColor, true);
            }

            
            int kindY = resistBaseY + 3 * ROW_H + 8;
            gui.drawString(this.font, tr("entity_kind"), px + 10, kindY + 5, 0xFF9FB0C0);
            
            int healY = kindY + ROW_H + 4;
            gui.drawString(this.font, tr("heal_mode_label"), px + 10, healY + 5, 0xFF9FB0C0);
        } else if (page == PAGE_SHIELD) {
            
            int row0 = cy0 + 8;
            
            int shieldY = row0;
            gui.drawString(this.font, tr("shield"), px + 10, shieldY + 5, 0xFF9FB0C0);
            String shieldText = shieldMax <= 0 ? tr("off") : fmt(dummy.getShieldValue()) + "/" + fmt(shieldMax);
            int sColor = shieldMax > 0 ? 0xFF55CCFF : 0xFF667788;
            gui.drawString(this.font, shieldText, px + 146 - this.font.width(shieldText), shieldY + 5, sColor, true);
            
            int regenY = shieldY + ROW_H;
            gui.drawString(this.font, tr("shield_regen"), px + 10, regenY + 5, 0xFF9FB0C0);
            String regenText = shieldMax <= 0 ? "-" : (shieldRegenStep * 5) + "%/s §8(" + shieldRegenStep + ")";
            gui.drawString(this.font, regenText, px + 146 - this.font.width(regenText), regenY + 5, 0xFF55CCFF, true);

            
            int delayY = regenY + ROW_H;
            gui.drawString(this.font, tr("shield_regen_delay"), px + 10, delayY + 5, 0xFF9FB0C0);
            String delayText = shieldMax <= 0 ? "-" : shieldRegenDelay + "t §8(" + String.format("%.1fs", shieldRegenDelay / 20.0F) + ")";
            gui.drawString(this.font, delayText, px + 146 - this.font.width(delayText), delayY + 5, 0xFF55CCFF, true);

            
            int onHitPctY = delayY + ROW_H;
            gui.drawString(this.font, tr("regen_on_hit_pct"), px + 10, onHitPctY + 5, 0xFF9FB0C0);
            String pctText2 = shieldMax <= 0 || !regenOnHit ? "-" : regenOnHitPct + "%";
            gui.drawString(this.font, pctText2, px + 146 - this.font.width(pctText2), onHitPctY + 5,
                    regenOnHitPct > 0 ? 0xFF55FF55 : 0xFF667788, true);

            
            int toughBaseY = onHitPctY + ROW_H + 4;
            for (TestDummy.DamageCategory cat : TestDummy.DamageCategory.values()) {
                int col = cat.index % 2;
                int row = cat.index / 2;
                int bx = px + 6 + col * 130;
                int by = toughBaseY + row * ROW_H;
                String name = tr("cat_" + cat.name().toLowerCase(Locale.ROOT));
                int tough = shieldToughness[cat.index];
                String toughText = tough + "×";
                
                int tColor = tough >= 100 ? 0xFFFF5555 : tough >= 20 ? 0xFFFFD700
                        : tough >= 5 ? 0xFF55FF55 : tough > 1 ? 0xFF55CCFF : 0xFF667788;
                gui.drawString(this.font, name, bx + 22, by + 5, 0xFFCCCCCC);
                gui.drawString(this.font, toughText, bx + 104 - this.font.width(toughText), by + 5, tColor, true);
            }

            
            int shieldSwY = toughBaseY + 3 * ROW_H + 12;
            gui.drawString(this.font, tr("regen_on_hit"), px + 6, shieldSwY - 11, 0xFF7F8C9B);
            gui.drawString(this.font, tr("shield_resist"), px + 138, shieldSwY - 11, 0xFF7F8C9B);
            
            gui.drawString(this.font, tr("shield_toughness"), px + 6, toughBaseY - 10, 0xFF7F8C9B);
            

            
            if (shieldMax <= 0) {
                gui.drawString(this.font, tr("shield_off_hint"), px + 10, row0 - 12, 0xFF7F8C9B);
            }
        } else {
            
            boolean dropdown = buffDropdownOpen;
            int listTop = dropdown ? cy0 + 140 : cy0 + 62;
            gui.drawString(this.font, tr("buff_active"), px + 6, listTop - 12, 0xFF88FF88);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    
    private void statCard(GuiGraphics gui, int x, int y, String label, String value, String note) {
        int cardH = note != null ? 40 : 32;
        
        gui.fill(x - 4, y - 4, x + 116, y + cardH, 0x80161E2C);
        gui.fill(x - 4, y - 4, x - 2, y + cardH, accentFor(label));
        gui.fill(x - 4, y - 4, x + 116, y - 3, 0x50CC8844);
        
        gui.drawString(this.font, label, x + 2, y, 0xFF7F8C9B);
        
        String shown = note != null ? value : value;
        int vColor = valueColor(value);
        gui.drawString(this.font, shown, x + 112 - this.font.width(shown), y + (note != null ? 0 : 11), vColor, true);
        if (note != null) {
            gui.drawString(this.font, note, x + 2, y + 12, 0xFF9FB0C0);
            
            gui.drawString(this.font, value, x + 112 - this.font.width(value), y + 24, vColor, true);
        }
    }

    
    private int accentFor(String label) {
        if (label.equals(tr("last_hit"))) return 0xFFFF5555;
        if (label.equals(tr("max_hit"))) return 0xFF55FFFF;
        if (label.equals(tr("dps"))) return 0xFFFFFF55;
        if (label.equals(tr("burst"))) return 0xFFFF55FF;
        if (label.equals(tr("hits"))) return 0xFF55FF55;
        return 0xFFFFAA00;         
    }

    
    private int valueColor(String v) {
        try {
            float f = Float.parseFloat(v.replace("M", "e6").replace("k", "e3"));
            if (f >= 100_000F || v.contains("M")) return 0xFFFF5555;
            if (f >= 1_000F || v.contains("k")) return 0xFFFFD700;
        } catch (NumberFormatException ignored) {
        }
        return 0xFFFFFFFF;
    }

    private String ellipsize(String line, int maxWidth) {
        if (this.font.width(line) <= maxWidth) return line;
        while (line.length() > 3 && this.font.width(line + "...") > maxWidth) {
            line = line.substring(0, line.length() - 1);
        }
        return line + "...";
    }

    private static String fmt(float v) {
        if (v >= 1_000_000F) return String.format("%.2fM", v / 1_000_000F);
        if (v >= 10_000F) return String.format("%.1fk", v / 1_000F);
        return String.format("%.1f", v);
    }

    private static String fmtRaw(float v) {
        return "原 " + fmt(v);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == PAGE_BUFFS) {
            if (buffDropdownOpen) {
                List<BuffRegistryList.BuffEntry> entries = BuffRegistryList.all();
                int maxScroll = Math.max(0, entries.size() - BUFF_VISIBLE);
                buffScroll = delta < 0 ? Math.min(maxScroll, buffScroll + 1) : Math.max(0, buffScroll - 1);
            } else {
                
                int maxScroll = Math.max(0, activeBuffs.size() - 7);
                activeBuffScroll = delta < 0 ? Math.min(maxScroll, activeBuffScroll + 1)
                        : Math.max(0, activeBuffScroll - 1);
            }
            layoutPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (buffLevelBox != null && buffLevelBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
            page = (page + 1) % PAGE_COUNT;
            layoutPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
