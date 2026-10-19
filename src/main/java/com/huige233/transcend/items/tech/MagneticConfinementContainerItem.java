package com.huige233.transcend.items.tech;

import com.huige233.transcend.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;


/** 将手持未激活黑洞种子装入可复用磁约束容器，并保存和清除装载状态。 */
public final class MagneticConfinementContainerItem extends Item {
    public static final String CONTENT = "ConfinementContent";
    public static final String SCHEMA = "SchemaVersion";
    public static final int CURRENT_SCHEMA = 2;
    public MagneticConfinementContainerItem() { super(new Properties().stacksTo(1).fireResistant()); }
    public static boolean isLoaded(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return stack.is(ModItems.magnetic_confinement_container.get()) && tag != null
                && tag.getBoolean(CONTENT) && tag.getInt(SCHEMA) == CURRENT_SCHEMA
                && tag.getString(BlackHoleSeedItem.STATE).equals("loaded")
                && tag.contains(BlackHoleSeedItem.NONCE);
    }
    public static boolean load(ItemStack container, ItemStack seed) {
        if (!container.is(ModItems.magnetic_confinement_container.get()) || isLoaded(container)
                || !BlackHoleSeedItem.isInactive(seed)) return false;
        CompoundTag target = container.getOrCreateTag();
        target.putBoolean(CONTENT, true);
        target.putInt(SCHEMA, CURRENT_SCHEMA);
        target.putString(BlackHoleSeedItem.STATE, "loaded");
        target.putLong(BlackHoleSeedItem.POWER, BlackHoleSeedItem.power(seed));
        CompoundTag source = seed.getTag();
        if (source != null && source.contains(BlackHoleSeedItem.NONCE)) target.putUUID(BlackHoleSeedItem.NONCE, source.getUUID(BlackHoleSeedItem.NONCE));
        else target.putUUID(BlackHoleSeedItem.NONCE, java.util.UUID.randomUUID());
        return true;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack container = player.getItemInHand(hand);
        if (!isLoaded(container)) {
            for (InteractionHand otherHand : InteractionHand.values()) {
                ItemStack seed = player.getItemInHand(otherHand);
                if (BlackHoleSeedItem.isInactive(seed)) {
                    if (!level.isClientSide) {
                        load(container, seed);
                        seed.shrink(1);
                    }
                    return InteractionResultHolder.sidedSuccess(container, level.isClientSide);
                }
            }
        }
        return InteractionResultHolder.pass(container);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.translatable(isLoaded(stack)
                ? "tooltip.transcend.magnetic_confinement_container.loaded"
                : "tooltip.transcend.magnetic_confinement_container.empty"));
    }

    public static void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove(CONTENT);
        tag.remove(SCHEMA);
        tag.remove(BlackHoleSeedItem.STATE);
        tag.remove(BlackHoleSeedItem.POWER);
        tag.remove(BlackHoleSeedItem.NONCE);
        if (tag.isEmpty()) stack.setTag(null);
    }
}
