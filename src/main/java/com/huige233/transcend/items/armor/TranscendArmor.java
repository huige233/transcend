package com.huige233.transcend.items.armor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.huige233.transcend.ModRarities;
import com.huige233.transcend.util.ArmorUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TranscendArmor extends ArmorItem {

    public TranscendArmor(Type type) {
        super(TranscendArmorMaterial.INSTANCE, type,
                new Properties().rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        if (ArmorUtils.fullEquipped(player)) {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);
            player.setAirSupply(300);
            player.fallDistance = 0;
            player.setRemainingFireTicks(0);

            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.getPersistentData().putBoolean("transcend_armor_flight", true);

            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 14, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 14, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 14, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 44, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 300, 9, false, false));

            if (player.getAbsorptionAmount() < 1000) {
                player.setAbsorptionAmount(2000);
            }
        } else if (getEquipmentSlot() == EquipmentSlot.CHEST
                && player.getPersistentData().getBoolean("transcend_armor_flight")) {
            player.getPersistentData().remove("transcend_armor_flight");
            if (!player.isCreative() && !player.isSpectator()
                    && !player.getPersistentData().getBoolean("transcend_curio_flight")) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        UUID uuid = new UUID(slot.toString().hashCode(), 0);

        if (slot == getEquipmentSlot()) {
            multimap.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, "Transcend Armor", 512, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.ARMOR, new AttributeModifier(uuid, "Transcend Armor", 1000, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Transcend Armor", 1000, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Transcend Armor", 1, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.LUCK, new AttributeModifier(uuid, "Transcend Armor", 250, AttributeModifier.Operation.ADDITION));
        }

        return multimap;
    }
}
