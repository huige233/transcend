package com.huige233.transcend.items.tech;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.shield.ShieldCluster;
import com.huige233.transcend.tech.shield.ShieldDefenseMode;
import com.huige233.transcend.tech.shield.ShieldInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

   
                                                                                               
                                                                                             
   
/** 实现可手持或佩戴的相位护盾物品，管理电容、护盾层数据迁移、模块参数与使用反馈。 */
public class PhaseShield extends Item implements ICurioItem {
    
    public static final String NBT_ENERGY = "PhaseEnergy";
    public static final float MIN_USE_ENERGY = 0.01F;
    public static final String NBT_CAPACITOR = "Capacitor";
    public static final String NBT_CAPACITOR_ENERGY = "Energy";
    public static final String NBT_CAPACITOR_MAX = "MaxEnergy";
    public static final int STANDARD_CAPACITOR_CAPACITY = 200;

    public PhaseShield() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.transcend.phase_shield").withStyle(ChatFormatting.DARK_AQUA));
        ShieldModule module = PhaseShieldModules.module(stack.getTag());
        if (module != null) {
            tooltip.add(Component.translatable("shieldmodule.transcend." + module.id()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.transcend.phase_shield.energy",
                String.format(java.util.Locale.ROOT, "%.0f", getEnergy(stack)), String.format(java.util.Locale.ROOT, "%.0f", getMaxEnergy(stack)))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.transcend.phase_shield.capacitor",
                String.format(java.util.Locale.ROOT, "%.0f", capacitorEnergy(stack)),
                String.format(java.util.Locale.ROOT, "%.0f", capacitorCapacity(stack)))
                .withStyle(capacitorEnergy(stack) > 0.0F ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.transcend.phase_shield.use").withStyle(ChatFormatting.GRAY));
    }

    
    public static float capacitorCapacity(ItemStack stack) {
        CompoundTag capacitor = capacitorTag(stack);
        return Math.max(0, capacitor.getInt(NBT_CAPACITOR_MAX));
    }

    public static float capacitorEnergy(ItemStack stack) {
        CompoundTag capacitor = capacitorTag(stack);
        return Math.max(0.0F, Math.min(capacitorCapacity(stack), capacitor.getInt(NBT_CAPACITOR_ENERGY)));
    }

    public static boolean hasCapacitor(ItemStack stack) {
        return capacitorCapacity(stack) > 0.0F;
    }

    public static void installStandardCapacitor(ItemStack stack) {
        CompoundTag capacitor = new CompoundTag();
        capacitor.putInt(NBT_CAPACITOR_MAX, STANDARD_CAPACITOR_CAPACITY);
        capacitor.putInt(NBT_CAPACITOR_ENERGY, STANDARD_CAPACITOR_CAPACITY);
        stack.getOrCreateTag().put(NBT_CAPACITOR, capacitor);
    }

    public static float drainCapacitor(ItemStack stack, float amount) {
        CompoundTag capacitor = capacitorTag(stack);
        int max = capacitor.getInt(NBT_CAPACITOR_MAX);
        if (max <= 0) return 0.0F;
        int before = Math.max(0, Math.min(max, capacitor.getInt(NBT_CAPACITOR_ENERGY)));
        int drained = Math.min(before, Math.max(0, (int) Math.ceil(amount)));
        capacitor.putInt(NBT_CAPACITOR_ENERGY, before - drained);
        return drained;
    }

    private static CompoundTag capacitorTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_CAPACITOR, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? tag.getCompound(NBT_CAPACITOR) : new CompoundTag();
    }

    public static ShieldCluster loadCluster(ItemStack stack) {
        ShieldCluster cluster = TechItemData.loadShieldCluster(stack);
        return cluster.isEmpty() ? defaultCluster(legacyEnergy(stack)) : cluster;
    }

    
    public static ShieldCluster getOrCreateCluster(ItemStack stack) {
        ShieldCluster cluster = TechItemData.loadShieldCluster(stack);
        if (!cluster.isEmpty()) {
            applyModuleToCluster(stack, cluster);
            return cluster;
        }
        cluster = defaultCluster(legacyEnergy(stack));
        if (!hasCapacitor(stack)) installStandardCapacitor(stack);
        TechItemData.saveShieldCluster(stack, cluster);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_ENERGY)) tag.remove(NBT_ENERGY);
        return cluster;
    }

    private static ShieldCluster defaultCluster(float legacyEnergy) {
        ShieldCluster cluster = new ShieldCluster();
        float max = (float) TechConfig.phaseShieldCapacity();
        float initial = legacyEnergy >= 0.0F ? Math.min(max, legacyEnergy) : max;
        
        
        cluster.add(new ShieldInstance("phase_mk1", max, initial, 0.0F,
                ShieldDefenseMode.omni(), TechConfig.shieldOmnidirectionalRatio()));
        return cluster;
    }

    private static float legacyEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_ENERGY) ? Math.max(0.0F, tag.getFloat(NBT_ENERGY)) : -1.0F;
    }

    public static float energyMultiplier(ItemStack stack) {
        return PhaseShieldModules.energyMultiplier(stack.getTag());
    }

    private static void applyModuleToCluster(ItemStack stack, ShieldCluster cluster) {
        if (cluster.layers().isEmpty()) return;
        ShieldModule module = PhaseShieldModules.module(stack.getTag());
        float targetCapacity = module == null ? (float) TechConfig.phaseShieldCapacity() : module.capacity();
        ShieldInstance generator = cluster.layers().get(0);
        if (generator.maxEnergy() != targetCapacity) {
            generator.setMaxEnergy(targetCapacity);
            TechItemData.saveShieldCluster(stack, cluster);
        }
        if (module != null) {
            generator.setTypeDefense(module.category(), module.typeReduction());
            TechItemData.saveShieldCluster(stack, cluster);
        }
    }

    public static void applyModule(ItemStack stack) {
        ShieldCluster cluster = getOrCreateCluster(stack);
        applyModuleToCluster(stack, cluster);
    }

    public static float getEnergy(ItemStack stack) {
        ShieldCluster cluster = loadCluster(stack);
        return Math.min(cluster.energy(), capacitorEnergy(stack));
    }

    public static float getMaxEnergy(ItemStack stack) {
        return loadCluster(stack).maxEnergy();
    }

    public static boolean tickRegen(ItemStack stack) {
        ShieldCluster cluster = getOrCreateCluster(stack);
        boolean changed = cluster.tickRegen();
        if (changed) TechItemData.saveShieldCluster(stack, cluster);
        return changed;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getEnergy(stack) < MIN_USE_ENERGY || !hasCapacitor(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("msg.transcend.phase_shield.no_energy")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.8F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
                             @NotNull LivingEntity entity, int timeLeft) {
        if (!level.isClientSide) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.3F, 1.4F);
        }
    }

    @Override public int getUseDuration(@NotNull ItemStack stack) { return 72000; }
    @Override public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.BLOCK; }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level,
                              @NotNull net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) getOrCreateCluster(stack);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!slotContext.entity().level().isClientSide) getOrCreateCluster(stack);
    }

    @Override public boolean isBarVisible(@NotNull ItemStack stack) { return hasCapacitor(stack) && capacitorEnergy(stack) < capacitorCapacity(stack); }
    @Override public int getBarWidth(@NotNull ItemStack stack) {
        return (int) (capacitorEnergy(stack) / Math.max(1.0F, capacitorCapacity(stack)) * 13.0F);
    }
    @Override public int getBarColor(@NotNull ItemStack stack) { return 0xFF00AACC; }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isFoil(@NotNull ItemStack stack) { return getEnergy(stack) >= getMaxEnergy(stack); }
    @Override public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack ingredient) { return false; }
}
