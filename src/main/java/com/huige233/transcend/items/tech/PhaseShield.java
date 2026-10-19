package com.huige233.transcend.items.tech;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.tech.TechConfig;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.shield.ShieldCluster;
import com.huige233.transcend.tech.shield.ShieldDefenseMode;
import com.huige233.transcend.tech.shield.ShieldInstance;
import com.huige233.transcend.tech.shield.TechShieldTierProfile;
import com.huige233.transcend.tech.shield.ShieldDamageContext;
import com.huige233.transcend.tech.shield.ShieldResolution;
import com.huige233.transcend.tech.rank.TechRank;
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
    public static final float OXYGEN_ENERGY_PER_TICK = 0.05F;
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
        TechShieldTierProfile profile = TechShieldTierProfile.of(getRank(stack));
        tooltip.add(Component.translatable("tooltip.transcend.phase_shield.rank", getRank(stack).name(),
                PhaseShieldModules.activeModules(stack).size(), profile.slots()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.transcend.phase_shield.reduction", Math.round(profile.reduction() * 100)));
        for (ShieldModule module : PhaseShieldModules.activeModules(stack)) {
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
        return finite(capacitor.getFloat(NBT_CAPACITOR_MAX));
    }

    public static float capacitorEnergy(ItemStack stack) {
        CompoundTag capacitor = capacitorTag(stack);
        return Math.min(capacitorCapacity(stack), finite(capacitor.getFloat(NBT_CAPACITOR_ENERGY)));
    }

    public static boolean hasCapacitor(ItemStack stack) {
        return capacitorCapacity(stack) > 0.0F;
    }

    public static void installStandardCapacitor(ItemStack stack) {
        CompoundTag capacitor = new CompoundTag();
        capacitor.putFloat(NBT_CAPACITOR_MAX, ratedCapacity(stack));
        capacitor.putFloat(NBT_CAPACITOR_ENERGY, ratedCapacity(stack));
        stack.getOrCreateTag().put(NBT_CAPACITOR, capacitor);
    }

    public static float drainCapacitor(ItemStack stack, float amount) {
        CompoundTag capacitor = capacitorTag(stack);
        float before = capacitorEnergy(stack);
        float drained = Math.min(before, finite(amount));
        if (drained > 0) capacitor.putFloat(NBT_CAPACITOR_ENERGY, before - drained);
        return drained;
    }

    private static CompoundTag capacitorTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_CAPACITOR, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? tag.getCompound(NBT_CAPACITOR) : new CompoundTag();
    }

    public static ShieldCluster loadCluster(ItemStack stack) {
        TechShieldTierProfile profile = TechShieldTierProfile.of(getRank(stack));
        ShieldCluster cluster = new ShieldCluster();
        // 特化层先吸收，普通层兜底；电容是唯一的有限能源池，发射器额定容量限制单次承载。
        for (ShieldModule module : PhaseShieldModules.activeModules(stack)) {
            if (cluster.layers().size() >= TechConfig.shieldClusterMaxLayers() - 1) break;
            if (module.typeReduction() <= 0) continue;
            ShieldInstance layer = new ShieldInstance("module_" + module.id(), profile.defenseTier(),
                    module.capacity(), module.capacity(), 0,
                    module.category() < 0 ? ShieldDefenseMode.omni() : ShieldDefenseMode.directional(module.category()),
                    module.category() < 0 ? TechConfig.shieldOmnidirectionalRatio() : TechConfig.shieldDirectionalRatio());
            layer.setTypeDefense(module.category(), module.typeReduction());
            layer.setDamageReduction(profile.reduction());
            cluster.add(layer);
        }
        float capacity = ratedCapacity(stack);
        ShieldInstance base = new ShieldInstance("phase_" + getRank(stack).id(), profile.defenseTier(),
                capacity, capacity, 0, profile.allTypes() ? ShieldDefenseMode.omni() : ShieldDefenseMode.conventional(),
                TechConfig.shieldOmnidirectionalRatio());
        base.setDamageReduction(profile.reduction());
        cluster.add(base);
        return cluster;
    }

    
    public static ShieldCluster getOrCreateCluster(ItemStack stack) {
        initialize(stack);
        return loadCluster(stack);
    }

    /** 只迁移和校准电容，不在每个背包 tick 重建护盾层。 */
    public static void initialize(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("ShieldInitialized")) {
            if (!hasCapacitor(stack)) {
                boolean fresh = TechItemData.loadShieldCluster(stack).isEmpty();
                float legacy = legacyEnergy(stack);
                installStandardCapacitor(stack);
                if (legacy >= 0) capacitorTag(stack).putFloat(NBT_CAPACITOR_ENERGY, Math.min(legacy, ratedCapacity(stack)));
                else if (!fresh) capacitorTag(stack).putFloat(NBT_CAPACITOR_ENERGY, 0);
            }
            tag.putBoolean("ShieldInitialized", true);
            tag.remove(NBT_ENERGY);
        }
        if (hasCapacitor(stack)) {
            float before = capacitorEnergy(stack);
            float capacity = ratedCapacity(stack);
            capacitorTag(stack).putFloat(NBT_CAPACITOR_MAX, capacity);
            // 升级和拆装只改变容量上限，不产生新能量。
            capacitorTag(stack).putFloat(NBT_CAPACITOR_ENERGY, Math.min(before, capacity));
        }
    }

    private static float legacyEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_ENERGY) ? Math.max(0.0F, tag.getFloat(NBT_ENERGY)) : -1.0F;
    }

    public static float energyMultiplier(ItemStack stack) {
        return PhaseShieldModules.energyMultiplier(stack) * (float) TechConfig.phaseShieldEfficiency();
    }

    private static float baseCapacity(ItemStack stack) {
        return TechShieldTierProfile.of(getRank(stack)).capacity() * (float) TechConfig.phaseShieldCapacity() / 200.0F;
    }

    private static float ratedCapacity(ItemStack stack) {
        return PhaseShieldModules.capacity(stack, baseCapacity(stack));
    }

    public static void applyModule(ItemStack stack) { initialize(stack); }

    public static ShieldResolution absorb(ItemStack stack, ShieldDamageContext context) {
        ShieldCluster cluster = getOrCreateCluster(stack);
        float multiplier = energyMultiplier(stack);
        // 同级及更高等级的场可承受科技穿盾；穿透转成有限能耗压力，避免同级第一发直接跳过整盾。
        if (context.attackingRank() != null && getRank(stack).ordinal() >= context.attackingRank().ordinal()) {
            int tier = TechShieldTierProfile.of(getRank(stack)).defenseTier();
            multiplier *= 1.0F + Math.min(1.0F, Math.max(0, context.penetrationStrength() - tier + 1) * 0.25F);
            context = new ShieldDamageContext(context.category(), context.amount(), context.bypassesShield(),
                    Math.min(context.penetrationStrength(), tier - 1), context.attackingRank());
        }
        ShieldResolution result = cluster.resolvePowered(context, capacitorEnergy(stack) / multiplier);
        drainCapacitor(stack, result.shieldEnergySpent() * multiplier);
        return result;
    }

    public static boolean covers(ItemStack stack, int category) {
        return loadCluster(stack).layers().stream().anyMatch(layer -> layer.defenseMode().protects(category));
    }

    public static boolean supplyOxygen(ItemStack stack, LivingEntity wearer) {
        if (wearer.level().isClientSide || !wearer.isUnderWater()) return false;
        initialize(stack);
        if (capacitorEnergy(stack) < OXYGEN_ENERGY_PER_TICK) return false;
        drainCapacitor(stack, OXYGEN_ENERGY_PER_TICK);
        wearer.setAirSupply(wearer.getMaxAirSupply());
        return true;
    }

    public static float getEnergy(ItemStack stack) { return capacitorEnergy(stack); }
    public static float getMaxEnergy(ItemStack stack) { return ratedCapacity(stack); }

    public static TechRank getRank(ItemStack stack) { return TechItemData.getRank(stack); }

    public static boolean allowsModule(ItemStack stack, ShieldModule module) {
        return module != null && getRank(stack).ordinal() >= module.minimumRank().ordinal();
    }

    public static boolean tickRegen(ItemStack stack) {
        return false; // 有限电容只能通过外部充电补能。
    }

    private static float finite(float value) { return Float.isFinite(value) ? Math.max(0, value) : 0; }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) initialize(stack);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ItemStack battery = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                battery.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).ifPresent(energy -> {
                    int missing = (int) Math.floor(capacitorCapacity(stack) - capacitorEnergy(stack));
                    int received = energy.extractEnergy(Math.max(0, missing), false);
                    if (received > 0) capacitorTag(stack).putFloat(NBT_CAPACITOR_ENERGY,
                            Math.min(capacitorCapacity(stack), capacitorEnergy(stack) + received));
                });
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
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
        if (!level.isClientSide) initialize(stack);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!slotContext.entity().level().isClientSide) initialize(stack);
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
