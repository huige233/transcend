package com.huige233.transcend.items.circle;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleFunctionExecutorRegistry;
import com.huige233.transcend.circle.CircleFunctionType;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.MagicCrystalItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 法阵函数拓印卷轴 — 一次性触发某个 CircleFunction 的效果。
 *
 * <p>NBT：
 * <ul>
 *   <li><b>FunctionId</b> (String) — CircleFunctionType 的 enum name</li>
 *   <li><b>Tier</b> (int) — 拓印源法阵的 tier (1-5)，影响效果强度</li>
 * </ul>
 *
 * <p>使用：右键空中或方块。
 * 客户端按 AncientSpellScrollItem 模式：消耗背包中等值的 magic_crystal，
 * 调用 CircleFunctionExecutor 的 onActivate + 一次 tick 来触发短促效果。
 */
public class FunctionImprintScrollItem extends Item {

    private static final String TAG_FUNCTION = "FunctionId";
    private static final String TAG_TIER = "Tier";
    private static final int COOLDOWN_TICKS = 80;

    public FunctionImprintScrollItem() {
        super(new Properties().stacksTo(8).rarity(Rarity.RARE));
        ModItems.ITEMS.add(this);
    }

    public static ItemStack of(CircleFunctionType type, int tier, Item self) {
        ItemStack stack = new ItemStack(self);
        bind(stack, type, tier);
        return stack;
    }

    public static void bind(ItemStack stack, CircleFunctionType type, int tier) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_FUNCTION, type.name());
        tag.putInt(TAG_TIER, Math.max(1, Math.min(5, tier)));
    }

    @Nullable
    public static CircleFunctionType getFunction(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FUNCTION)) return null;
        try {
            return CircleFunctionType.valueOf(tag.getString(TAG_FUNCTION));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static int getTier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 1;
        int t = tag.getInt(TAG_TIER);
        return t >= 1 && t <= 5 ? t : 1;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return getFunction(stack) != null;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                            @NotNull Player player,
                                                            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        CircleFunctionType type = getFunction(stack);
        if (type == null) {
            sp.displayClientMessage(
                    Component.translatable("msg.transcend.imprint_scroll.no_function")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        CircleFunctionExecutor executor = CircleFunctionExecutorRegistry.get(type);
        if (executor == null) {
            sp.displayClientMessage(
                    Component.translatable("msg.transcend.imprint_scroll.no_executor", type.name())
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        int tier = getTier(stack);
        CircleTier ct = CircleTier.fromLevel(tier);

        // 消耗水晶: 函数基础 upkeep × 2 (作为一次性瞬发的代价)
        int manaCost = Math.max(8, Math.round(type.getBaseUpkeepPerMinute() * 2));
        if (!consumeCrystals(sp, manaCost)) {
            sp.displayClientMessage(
                    Component.translatable("scroll.transcend.not_enough_crystals", manaCost)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // 构造一个临时上下文：玩家自己的位置作为虚拟核心位置；魔力按 tier 容量
        int capacity = ct.getManaCapacity();
        CircleFunctionContext ctx = new CircleFunctionContext(
                serverLevel, sp.blockPosition(), ct, type, sp.getUUID(),
                capacity, capacity, ct.getThroughputPerMinute(),
                new ArrayList<>(), 0, 0, 0, 0);

        // 触发一次：onActivate + 一次 tick + onDeactivate （瞬时效果）
        if (!executor.canActivate(ctx)) {
            sp.displayClientMessage(
                    Component.translatable("msg.transcend.imprint_scroll.cannot_activate")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        executor.onActivate(ctx);
        executor.tick(ctx);
        executor.onDeactivate(ctx);

        // 反馈
        serverLevel.playSound(null, sp.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.9F);
        sp.displayClientMessage(
                Component.translatable("msg.transcend.imprint_scroll.triggered",
                        Component.translatable(type.getTranslationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        if (!sp.getAbilities().instabuild) {
            stack.shrink(1);
        }
        sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * @deprecated 替换为 {@link com.huige233.transcend.client.magic.MagicCrystalHelper#consumeMana(net.minecraft.world.entity.player.Player, int)}，
     *   统一抽取顺序 innate → storage → crystals。
     */
    @Deprecated
    private static boolean consumeCrystals(ServerPlayer player, int cost) {
        return com.huige233.transcend.client.magic.MagicCrystalHelper.consumeMana(player, cost);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CircleFunctionType type = getFunction(stack);
        int tier = getTier(stack);

        tooltip.add(Component.translatable("tooltip.transcend.imprint_scroll.desc")
                .withStyle(ChatFormatting.GRAY));
        if (type != null) {
            tooltip.add(Component.translatable("tooltip.transcend.imprint_scroll.function",
                    Component.translatable(type.getTranslationKey()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.transcend.imprint_scroll.tier",
                    Component.translatable(CircleTier.fromLevel(tier).getTranslationKey()))
                    .withStyle(ChatFormatting.GOLD));
            int manaCost = Math.max(8, Math.round(type.getBaseUpkeepPerMinute() * 2));
            tooltip.add(Component.translatable("tooltip.transcend.imprint_scroll.cost", manaCost)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.transcend.imprint_scroll.empty")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
