package com.huige233.transcend.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.huige233.transcend.ModRarities;
import com.huige233.transcend.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TranscendShield extends Item {

    private static final UUID OFF_HAND_UUID = UUID.fromString("9271eeea-5f74-4e12-97b6-7cf3c60ef7a0");
    private static final UUID MAIN_HAND_UUID = UUID.fromString("7d766720-0695-46c6-b320-44529f3da63f");
    public static final int TIMESTOP_TICKS = 10 * 20;

    public TranscendShield() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal(TextUtils.makeFabulous("Transcend Shield")));
        tooltip.add(Component.translatable("tooltip.transcend.shield.timestop").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.transcend.shield.reflect").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.transcend.shield.immortal").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 72000;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public static void applyTimeStop(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TIMESTOP_TICKS, 255, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, TIMESTOP_TICKS, 255, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, TIMESTOP_TICKS, 128, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TIMESTOP_TICKS, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TIMESTOP_TICKS, 255, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, TIMESTOP_TICKS, 0, false, false));

        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        target.setNoGravity(true);

        if (!target.level().isClientSide) {
            target.getPersistentData().putInt("transcend_timestop", TIMESTOP_TICKS);
        }
    }

    public static void reflectDamage(LivingEntity target, Player player) {
        if (target == null) return;
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.invulnerableTime = 0;
        target.hurt(target.damageSources().playerAttack(player), damage);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (!isHoldingShield(player)) return;

        if (player.isDeadOrDying()) {
            player.setHealth(player.getMaxHealth());
        }

        if (player.isUsingItem() && player.getUseItem().getItem() instanceof TranscendShield) {
            List<MobEffectInstance> negatives = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (!effect.getEffect().isBeneficial()) {
                    negatives.add(effect);
                }
            }
            for (MobEffectInstance negative : negatives) {
                player.removeEffect(negative.getEffect());
            }
        }
    }

    private static boolean isHoldingShield(Player player) {
        return player.getOffhandItem().getItem() instanceof TranscendShield
                || player.getMainHandItem().getItem() instanceof TranscendShield;
    }

    public static boolean hasTranscendShield(Player player) {
        if (player.getOffhandItem().getItem() instanceof TranscendShield) return true;
        if (player.getMainHandItem().getItem() instanceof TranscendShield) return true;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof TranscendShield) return true;
        }
        return false;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack ingredient) {
        return false;
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
        if (slot == EquipmentSlot.OFFHAND) {
            multimap.put(Attributes.ARMOR, new AttributeModifier(OFF_HAND_UUID, "Shield modifier", 20, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(OFF_HAND_UUID, "Shield modifier", 10, AttributeModifier.Operation.ADDITION));
        }
        if (slot == EquipmentSlot.MAINHAND) {
            multimap.put(Attributes.ARMOR, new AttributeModifier(MAIN_HAND_UUID, "Shield modifier", 20, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(MAIN_HAND_UUID, "Shield modifier", 10, AttributeModifier.Operation.ADDITION));
        }
        return multimap;
    }
}
