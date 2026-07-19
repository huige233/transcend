package com.huige233.transcend.client;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.items.TranscendWand;
import com.huige233.transcend.network.C2SWandCardEdit;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.config.SpellConfigurationRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CardProgramScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int HEIGHT = 220;

    private final List<Integer> wandInventorySlots = new ArrayList<>();
    private final List<SpellEffect> effects = new ArrayList<>();
    private int wandIndex;
    private int spellSlot;
    private int carrierIndex;
    private int elementIndex;
    private int nextEffectIndex;
    private int left;
    private int top;
    private Component compatibilityMessage;

    public static void open() {
        Minecraft.getInstance().setScreen(new CardProgramScreen());
    }

    public CardProgramScreen() {
        super(Component.translatable("gui.transcend.spell_config.title"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        findWands();
        if (!wandInventorySlots.isEmpty()) loadSlot();

        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.wand"), b -> cycleWand())
                .pos(left + 12, top + 28).size(62, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.slot"), b -> cycleSlot())
                .pos(left + 80, top + 28).size(62, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.carrier"), b -> cycleCarrier())
                .pos(left + 12, top + 58).size(90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.element"), b -> cycleElement())
                .pos(left + 108, top + 58).size(90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.add_effect"), b -> addEffect())
                .pos(left + 12, top + 90).size(90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.next_effect"), b -> cycleNextEffect())
                .pos(left + 108, top + 90).size(90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.remove_last"), b -> removeEffect())
                .pos(left + 204, top + 90).size(105, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.save"), b -> save())
                .pos(left + 204, top + 184).size(50, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.transcend.spell_config.close"), b -> onClose())
                .pos(left + 260, top + 184).size(50, 18).build());
    }

    private void findWands() {
        wandInventorySlots.clear();
        if (minecraft == null || minecraft.player == null) return;
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof TranscendWand) wandInventorySlots.add(i);
        }
    }

    private ItemStack currentWand() {
        if (minecraft == null || minecraft.player == null || wandInventorySlots.isEmpty()) return ItemStack.EMPTY;
        return minecraft.player.getInventory().getItem(wandInventorySlots.get(wandIndex));
    }

    private int currentWandSlots() {
        return Math.max(1, currentWand().getOrCreateTag().getInt("max_slots"));
    }

    private void loadSlot() {
        effects.clear();
        ItemStack wand = currentWand();
        if (wand.isEmpty()) return;
        ListTag slots = wand.getOrCreateTag().getList("wand_slots", Tag.TAG_COMPOUND);
        if (spellSlot >= slots.size()) spellSlot = 0;
        CompoundTag slot = spellSlot < slots.size() ? slots.getCompound(spellSlot) : new CompoundTag();
        SpellCarrier carrier = SpellCarrier.getById(slot.getString("carrier"));
        SpellElement element = SpellElement.getById(slot.getString("element"));
        carrierIndex = Math.max(0, java.util.Arrays.asList(SpellCarrier.values()).indexOf(carrier));
        elementIndex = Math.max(0, java.util.Arrays.asList(SpellElement.values()).indexOf(element));
        if (slot.contains("effects", Tag.TAG_LIST)) {
            ListTag list = slot.getList("effects", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(7, list.size()); i++) {
                SpellEffect effect = SpellEffect.getById(list.getString(i));
                if (effect != null) effects.add(effect);
            }
        }
    }

    private void cycleWand() {
        if (wandInventorySlots.isEmpty()) return;
        wandIndex = (wandIndex + 1) % wandInventorySlots.size();
        spellSlot = 0;
        loadSlot();
    }

    private void cycleSlot() {
        if (wandInventorySlots.isEmpty()) return;
        spellSlot = (spellSlot + 1) % currentWandSlots();
        loadSlot();
    }

    private void cycleCarrier() {
        carrierIndex = (carrierIndex + 1) % SpellCarrier.values().length;
        compatibilityMessage = SpellConfigurationRules.areEffectsCompatible(currentCarrier(), effects)
                ? null : Component.translatable("gui.transcend.spell_config.incompatible");
    }

    private void cycleElement() {
        elementIndex = (elementIndex + 1) % SpellElement.values().length;
    }

    private void cycleNextEffect() {
        SpellEffect[] values = SpellEffect.configurableValues();
        for (int i = 0; i < values.length; i++) {
            nextEffectIndex = (nextEffectIndex + 1) % values.length;
            if (SpellConfigurationRules.isEffectCompatible(currentCarrier(), values[nextEffectIndex])) break;
        }
        compatibilityMessage = null;
    }

    private void addEffect() {
        if (minecraft == null || minecraft.player == null) return;
        int unlocked = AscensionCapability.get(minecraft.player).getMaxSpellEffectSlots();
        if (effects.size() >= Math.min(7, unlocked)) return;
        SpellEffect effect = SpellEffect.configurableValues()[nextEffectIndex];
        if (!SpellConfigurationRules.isEffectCompatible(currentCarrier(), effect)) {
            compatibilityMessage = Component.translatable("gui.transcend.spell_config.incompatible");
            return;
        }
        if (!effect.isRepeatable() && effects.contains(effect)) return;
        effects.add(effect);
        compatibilityMessage = null;
    }

    private void removeEffect() {
        if (!effects.isEmpty()) effects.remove(effects.size() - 1);
    }

    private void save() {
        if (wandInventorySlots.isEmpty()) return;
        if (!SpellConfigurationRules.areEffectsCompatible(currentCarrier(), effects)) {
            compatibilityMessage = Component.translatable("gui.transcend.spell_config.incompatible");
            return;
        }
        NetworkHandler.CHANNEL.sendToServer(new C2SWandCardEdit(
                wandInventorySlots.get(wandIndex), spellSlot,
                SpellCarrier.values()[carrierIndex], SpellElement.values()[elementIndex], effects));
    }

    private SpellCarrier currentCarrier() {
        return SpellCarrier.values()[carrierIndex];
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xE0101018);
        graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF9B8450);
        graphics.drawString(font, title, left + 12, top + 10, 0xFFFFD77A);
        if (wandInventorySlots.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.no_wand"), left + 12, top + 54, 0xFFFF5555);
        } else {
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.selection",
                    wandInventorySlots.get(wandIndex), spellSlot + 1, currentWandSlots()), left + 12, top + 48, 0xFFBBBBBB);
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.carrier_value",
                    Component.translatable(SpellCarrier.values()[carrierIndex].getDisplayKey())),
                    left + 12, top + 80, 0xFFE6C76A);
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.element_value",
                    Component.translatable(SpellElement.values()[elementIndex].getDisplayKey())),
                    left + 108, top + 80, 0xFF66CCFF);
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.next_value",
                    Component.translatable(SpellEffect.configurableValues()[nextEffectIndex].getDisplayKey())),
                    left + 12, top + 114, 0xFFAAAAAA);
            int maxEffects = minecraft != null && minecraft.player != null
                    ? AscensionCapability.get(minecraft.player).getMaxSpellEffectSlots() : 1;
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.effects", effects.size(), maxEffects),
                    left + 12, top + 130, 0xFFFFFFFF);
            SpellConfigurationRules.Computation computation = SpellConfigurationRules.compute(
                    SpellCarrier.values()[carrierIndex], SpellElement.values()[elementIndex], effects);
            graphics.drawString(font, Component.translatable("gui.transcend.spell_config.summary",
                    computation.tier(), computation.manaCost()),
                    left + 204, top + 114, 0xFFFFD77A);
            for (int i = 0; i < effects.size(); i++) {
                graphics.drawString(font, Component.translatable("gui.transcend.spell_config.effect_entry", i + 1,
                                Component.translatable(effects.get(i).getDisplayKey())),
                        left + 18 + (i / 4) * 145, top + 144 + (i % 4) * 11, 0xFFD8B4FF);
            }
            if (compatibilityMessage != null) {
                graphics.drawString(font, compatibilityMessage, left + 12, top + 204, 0xFFFF5555);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
