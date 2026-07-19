package com.huige233.transcend.items.armor;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class ElementArmor extends ArmorItem {

    public enum ElementSet {
        PYRO("pyro", ElementArmorMaterial.PYRO, Set.of("fire", "blood"),
                ChatFormatting.RED, 0.25F, 0.15F),
        CRYO("cryo", ElementArmorMaterial.CRYO, Set.of("ice", "wind"),
                ChatFormatting.AQUA, 0.25F, 0.15F),
        STORM("storm", ElementArmorMaterial.STORM, Set.of("thunder", "sonic"),
                ChatFormatting.YELLOW, 0.20F, 0.20F),
        TERRA("terra", ElementArmorMaterial.TERRA, Set.of("earth", "nature", "poison"),
                ChatFormatting.DARK_GREEN, 0.20F, 0.10F),
        ARCANE("arcane_set", ElementArmorMaterial.ARCANE, Set.of("holy", "light", "time", "space"),
                ChatFormatting.GOLD, 0.15F, 0.25F),
        ABYSS("abyss", ElementArmorMaterial.ABYSS, Set.of("void", "dark", "eldritch", "chaos"),
                ChatFormatting.DARK_PURPLE, 0.30F, 0.10F);

        public final String id;
        public final ArmorMaterial material;
        public final Set<String> boostedElements;
        public final ChatFormatting color;
        public final float damageBoost;
        public final float costReduction;

        ElementSet(String id, ArmorMaterial material, Set<String> boostedElements,
                   ChatFormatting color, float damageBoost, float costReduction) {
            this.id = id;
            this.material = material;
            this.boostedElements = boostedElements;
            this.color = color;
            this.damageBoost = damageBoost;
            this.costReduction = costReduction;
        }
    }

    private final ElementSet elementSet;

    public ElementArmor(ElementSet set, Type type) {
        super(set.material, type, new Properties().stacksTo(1).fireResistant());
        this.elementSet = set;
        ModItems.ITEMS.add(this);
    }

    public ElementSet getElementSet() {
        return elementSet;
    }

    public static int countSetPieces(Player player, ElementSet set) {
        int count = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof ElementArmor ea && ea.elementSet == set) {
                count++;
            }
        }
        return count;
    }

    public static float getElementBoost(Player player, SpellElement element) {
        float totalBoost = 0;
        for (ElementSet set : ElementSet.values()) {
            if (set.boostedElements.contains(element.id)) {
                int pieces = countSetPieces(player, set);
                if (pieces >= 4) {
                    totalBoost += set.damageBoost * 2.0F;
                } else if (pieces >= 2) {
                    totalBoost += set.damageBoost * (0.5F * pieces);
                }
            }
        }
        return totalBoost;
    }

    public static float getCostReduction(Player player, SpellElement element) {
        float totalReduction = 0;
        for (ElementSet set : ElementSet.values()) {
            if (set.boostedElements.contains(element.id)) {
                int pieces = countSetPieces(player, set);
                if (pieces >= 2) {
                    totalReduction += set.costReduction * pieces * 0.5F;
                }
            }
        }
        return Math.min(0.6F, totalReduction);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (getEquipmentSlot() != EquipmentSlot.FEET) return;

        int pieces = countSetPieces(player, elementSet);
        if (pieces < 2) return;

        if (pieces >= 2) {
            switch (elementSet) {
                case PYRO -> player.setRemainingFireTicks(0);
                case CRYO -> player.setTicksFrozen(0);
                default -> {}
            }
        }

        if (pieces >= 4) {
            switch (elementSet) {
                case STORM -> {
                    if (player.tickCount % 20 == 0)
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, false, false));
                }
                case TERRA -> {
                    if (player.tickCount % 20 == 0)
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, false, false));
                }
                case ARCANE -> {
                    if (player.tickCount % 40 == 0)
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 0, false, false));
                }
                case ABYSS -> {
                    if (player.tickCount % 20 == 0)
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
                }
                default -> {}
            }
        }
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.element_armor." + elementSet.id)
                .withStyle(elementSet.color));

        tooltip.add(Component.translatable("tooltip.transcend.element_armor.boost",
                        (int)(elementSet.damageBoost * 100))
                .withStyle(ChatFormatting.GREEN));

        StringBuilder elements = new StringBuilder();
        for (String el : elementSet.boostedElements) {
            if (elements.length() > 0) elements.append(", ");
            elements.append(Component.translatable("spell.element." + el).getString());
        }
        tooltip.add(Component.translatable("tooltip.transcend.element_armor.elements", elements.toString())
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.transcend.element_armor.set2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.element_armor.set4")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
