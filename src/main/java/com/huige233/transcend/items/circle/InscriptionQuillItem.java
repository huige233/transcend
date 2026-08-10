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

/** 刻写羽毛笔物品（法阵）。 */
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
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe)) {
            return InteractionResult.PASS;
        }
        if (!coreBe.claimOrAuthorize(serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ItemStack quill = ctx.getItemInHand();
        boolean wantFunction = player.isShiftKeyDown();

        if (wantFunction) {
            return imprintFunction(serverLevel, serverPlayer, coreBe, quill);
        }
        return imprintStructure(serverLevel, serverPlayer, coreBe, quill);
    }

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

            giveBack(player, new ItemStack(Items.PAPER));
            player.displayClientMessage(
                    Component.translatable("scroll.transcend.not_enough_crystals", STRUCTURE_MANA_COST)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        ItemStack scroll = StructureBlueprintScrollItem.of(tier.getLevel(),
                ModItems.structure_blueprint_scroll.get());
        giveOrDrop(player, scroll);

        quill.hurtAndBreak(1, player, p -> {});
        level.playSound(null, core.getBlockPos(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.quill.structure_imprinted",
                        Component.translatable(tier.getTranslationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        return InteractionResult.CONSUME;
    }

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
