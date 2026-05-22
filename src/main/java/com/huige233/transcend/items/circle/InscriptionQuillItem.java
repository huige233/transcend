package com.huige233.transcend.items.circle;

import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleFunctionType;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.MagicCrystalItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 刻印羽笔 — 在已搭建的法环核心上拓印结构或函数到卷轴。
 *
 * <p>消耗:
 * <ul>
 *   <li>每次拓印消耗背包中 1 张"羊皮纸" ({@link Items#PAPER})。</li>
 *   <li>结构拓印消耗 32 CM 等值的魔力水晶。</li>
 *   <li>函数拓印消耗按函数 baseUpkeep×3 折算的魔力水晶。</li>
 *   <li>羽笔本体每次扣 1 点耐久 (默认 64 点)。</li>
 * </ul>
 *
 * <p>交互:
 * <ul>
 *   <li><b>右键已搭建的法环核心</b>: 拓印结构 → 输出
 *       {@link StructureBlueprintScrollItem} (携带核心当前 tier)。</li>
 *   <li><b>Shift + 右键已安装符印的法环核心</b>: 拓印函数 → 输出
 *       {@link FunctionImprintScrollItem} (携带核心当前函数 + tier)。</li>
 * </ul>
 *
 * <p>设计参照 Iron's Spells & Spellbooks 中"羽笔 + 卷轴 + 法术源 → 拓印卷轴"
 * 的工作流，但简化为单个工具直接对法环核心使用。
 */
public class InscriptionQuillItem extends Item {

    private static final int STRUCTURE_MANA_COST = 32;
    private static final int FUNCTION_MANA_MULTIPLIER = 3;

    public InscriptionQuillItem() {
        super(new Properties().stacksTo(1).durability(64).rarity(Rarity.UNCOMMON));
        ModItems.ITEMS.add(this);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MagicCircleCoreBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe)) {
            return InteractionResult.PASS;
        }

        ItemStack quill = ctx.getItemInHand();
        boolean wantFunction = player.isShiftKeyDown();

        if (wantFunction) {
            return imprintFunction(serverLevel, (ServerPlayer) player, coreBe, quill);
        }
        return imprintStructure(serverLevel, (ServerPlayer) player, coreBe, quill);
    }

    // ============================================================
    // 结构拓印
    // ============================================================

    private InteractionResult imprintStructure(ServerLevel level, ServerPlayer player,
                                                MagicCircleCoreBlockEntity core, ItemStack quill) {
        if (!core.isStructureValid() || core.getDetectedTier() == null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.quill.structure_invalid")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        CircleTier tier = core.getDetectedTier();

        if (!consumePaper(player, 1)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.quill.no_paper")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!consumeCrystals(player, STRUCTURE_MANA_COST)) {
            // 退还纸 (TODO: 也可以选择不退还，惩罚性消耗)
            giveBack(player, new ItemStack(Items.PAPER));
            player.displayClientMessage(
                    Component.translatable("scroll.transcend.not_enough_crystals", STRUCTURE_MANA_COST)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 产出卷轴
        ItemStack scroll = StructureBlueprintScrollItem.of(tier.getLevel(),
                ModItems.structure_blueprint_scroll.get());
        giveOrDrop(player, scroll);

        // 耐久 + 反馈
        quill.hurtAndBreak(1, player, p -> {});
        level.playSound(null, core.getBlockPos(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.quill.structure_imprinted",
                        Component.translatable(tier.getTranslationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        return InteractionResult.CONSUME;
    }

    // ============================================================
    // 函数拓印
    // ============================================================

    private InteractionResult imprintFunction(ServerLevel level, ServerPlayer player,
                                               MagicCircleCoreBlockEntity core, ItemStack quill) {
        CircleFunctionType type = core.getActiveFunction();
        if (type == null || !core.isSigilLocked()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.quill.no_function")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        CircleTier tier = core.getDetectedTier() != null ? core.getDetectedTier() : CircleTier.fromLevel(1);
        int cost = Math.max(16, Math.round(type.getBaseUpkeepPerMinute() * FUNCTION_MANA_MULTIPLIER));

        if (!consumePaper(player, 1)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.quill.no_paper")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!consumeCrystals(player, cost)) {
            giveBack(player, new ItemStack(Items.PAPER));
            player.displayClientMessage(
                    Component.translatable("scroll.transcend.not_enough_crystals", cost)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 产出卷轴
        ItemStack scroll = FunctionImprintScrollItem.of(type, tier.getLevel(),
                ModItems.function_imprint_scroll.get());
        giveOrDrop(player, scroll);

        quill.hurtAndBreak(1, player, p -> {});
        level.playSound(null, core.getBlockPos(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.quill.function_imprinted",
                        Component.translatable(type.getTranslationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        return InteractionResult.CONSUME;
    }

    // ============================================================
    // 工具
    // ============================================================

    private static boolean consumePaper(Player player, int count) {
        Inventory inv = player.getInventory();
        int found = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && s.is(Items.PAPER)) {
                found += s.getCount();
                if (found >= count) break;
            }
        }
        if (found < count) return false;

        int remaining = count;
        for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty() || !s.is(Items.PAPER)) continue;
            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;
        }
        return true;
    }

    private static boolean consumeCrystals(Player player, int cost) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && s.getItem() instanceof MagicCrystalItem crystal) {
                total += crystal.getCrystalValue() * s.getCount();
                if (total >= cost) break;
            }
        }
        if (total < cost) return false;

        int remaining = cost;
        for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty() || !(s.getItem() instanceof MagicCrystalItem crystal)) continue;
            int unit = crystal.getCrystalValue();
            if (unit <= 0) continue;
            int needed = (remaining + unit - 1) / unit;
            int take = Math.min(needed, s.getCount());
            s.shrink(take);
            remaining -= take * unit;
        }
        return true;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void giveBack(Player player, ItemStack stack) {
        // 简单退还：直接尝试放进背包
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() / 2;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.quill.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.quill.use_core")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.quill.shift_use_core")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
