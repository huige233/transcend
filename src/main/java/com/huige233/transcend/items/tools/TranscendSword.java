package com.huige233.transcend.items.tools;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.huige233.transcend.*;
import com.huige233.transcend.entity.RainbowLightning;
import com.huige233.transcend.util.SwordUtil;
import com.huige233.transcend.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TranscendSword extends SwordItem {

    private static final String TAG_DESTRUCTION = "Destruction";
    private static final int AOE_RANGE = 50;

    public TranscendSword() {
        super(ModToolTiers.TRANSCEND, 900, 0f,
                (new Properties()).rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    public static boolean isDestructionMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_DESTRUCTION);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel,
                                List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        String line1 = TextUtils.makeFabulous("Everything");
        String line2 = TextUtils.makeFabulous("Destruction");
        pTooltipComponents.add(Component.literal(line1 + " " + line2));

        if (Screen.hasShiftDown()) {
            pTooltipComponents.add(Component.literal(
                    "Ascendit a terra in coelum, iterumque descendit in terram, "
                            + "et recipit vim superiorum et inferiorum.")
                    .withStyle(ChatFormatting.BLUE));
        } else {
            pTooltipComponents.add(Component.literal("Sic mundus creatus est.")
                    .withStyle(ChatFormatting.GREEN));
        }

        if (isDestructionMode(pStack)) {
            pTooltipComponents.add(Component.literal("")
                    .append(Component.translatable("tooltip.transcend.mode.destruction")
                            .withStyle(ChatFormatting.RED)));
        }

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (isDestructionMode(stack)) {
            return Component.translatable("item.transcend.transcend_sword.destruction")
                    .withStyle(ChatFormatting.GOLD);
        }
        return super.getName(stack);
    }

    public static void sweepAttack(Level level, LivingEntity livingEntity, Entity victim) {
        if (livingEntity instanceof Player player) {
            for (LivingEntity livingentity : level.getEntitiesOfClass(LivingEntity.class,
                    player.getItemInHand(InteractionHand.MAIN_HAND).getSweepHitBox(player, victim))) {
                double entityReachSq = Mth.square(player.getEntityReach());
                if (!player.isAlliedTo(livingentity)
                        && (!(livingentity instanceof ArmorStand as) || !as.isMarker())
                        && player.distanceToSqr(livingentity) < entityReachSq) {
                    livingentity.knockback(0.6F,
                            Mth.sin(player.getYRot() * ((float) Math.PI / 180F)),
                            -Mth.cos(player.getYRot() * ((float) Math.PI / 180F)));
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
            double d0 = -Mth.sin(player.getYRot() * ((float) Math.PI / 180F));
            double d1 = Mth.cos(player.getYRot() * ((float) Math.PI / 180F));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX() + d0, player.getY(0.5D), player.getZ() + d1,
                        0, d0, 0.0D, d1, 0.0D);
            }
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide) {
            if (target instanceof LivingEntity living) {
                sweepAttack(player.level(), player, living);
            }

//            if (player.level() instanceof ServerLevel serverLevel) {
//                spawnLightning(serverLevel, target);
//            }

            SwordUtil.annihilate(target, player);
        }
        return false;
    }

//    private static void spawnLightning(ServerLevel level, Entity target) {
//        for (int i = 0; i < 5; i++) {
//            RainbowLightning bolt = new RainbowLightning(level,
//                    target.getX(), target.getY(), target.getZ());
//            level.addFreshEntity(bolt);
//        }
//    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (!player.isShiftKeyDown()) {
                boolean destruction = isDestructionMode(stack);
                stack.getOrCreateTag().putBoolean(TAG_DESTRUCTION, !destruction);
                if (!destruction) {
                    player.displayClientMessage(
                            Component.translatable("tooltip.transcend.mode.destruction.on")
                                    .withStyle(ChatFormatting.RED), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("tooltip.transcend.mode.normal.on")
                                    .withStyle(ChatFormatting.GREEN), true);
                }
                player.swing(hand);
            } else {
                if (isDestructionMode(stack)) {
                    if (player.isSprinting()) {
                        int count = SwordUtil.removeAllEntities(level, player);
                        player.displayClientMessage(
                                Component.literal("EntityRemove: " + count)
                                        .withStyle(ChatFormatting.DARK_RED), false);
                    } else {
                        int count = SwordUtil.killRange(level, player, AOE_RANGE);
                        player.displayClientMessage(
                                Component.translatable("transcend.sword.ranger_kill", AOE_RANGE, count)
                                        .withStyle(ChatFormatting.GOLD), false);
                    }
                }
            }
        }
        return InteractionResultHolder.success(stack);
    }

    public static DamageSource transcend(Level level, Entity attacker) {
        Holder<DamageType> holder =
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageTypes.TRANSCEND);
        return new DamageSource(holder, attacker);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack pStack) {
        return false;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        if (slot == EquipmentSlot.MAINHAND) {
            UUID uuid = new UUID((slot.toString()).hashCode(), 0);
            multimap.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    getTier().getAttackDamageBonus(), AttributeModifier.Operation.ADDITION));
            multimap.put(TranscendAttributes.TRANSCEND_DAMAGE.get(), new AttributeModifier(
                    uuid, "Weapon modifier",
                    getTier().getAttackDamageBonus(), AttributeModifier.Operation.ADDITION));
            multimap.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                    uuid, "Weapon modifier",
                    256, AttributeModifier.Operation.ADDITION));
            multimap.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                    BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                    getTier().getSpeed(), AttributeModifier.Operation.ADDITION));
        }
        return multimap;
    }
}
