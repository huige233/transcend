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
    public MagneticConfinementContainerItem() { super(new Properties().stacksTo(1).fireResistant()); }
    public static boolean isLoaded(ItemStack stack) { return stack.getOrCreateTag().getBoolean(CONTENT); }
    public static boolean load(ItemStack container, ItemStack seed) {
        if (!container.is(ModItems.magnetic_confinement_container.get()) || isLoaded(container)
                || !BlackHoleSeedItem.isInactive(seed)) return false;
        container.getOrCreateTag().putBoolean(CONTENT, true);
        container.getOrCreateTag().putInt("SchemaVersion", 1);
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
        tag.remove("SchemaVersion");
        if (tag.isEmpty()) stack.setTag(null);
    }
}