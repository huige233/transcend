package com.huige233.transcend.items.forge;

import com.huige233.transcend.gear.GearForgeData;
import com.huige233.transcend.gear.forge.TriggerAffixKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TriggerInscriptionItem extends Item {

    private final TriggerAffixKind kind;

    public TriggerInscriptionItem(TriggerAffixKind kind) {
        super(new Properties().rarity(Rarity.RARE).stacksTo(16));
        this.kind = kind;
    }

    public TriggerAffixKind getKind() { return kind; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        ItemStack inscription = player.getItemInHand(hand);
        ItemStack target = player.getOffhandItem();

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.shift_hint")
                                .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.pass(inscription);
        }

        if (target.isEmpty() || !GearForgeData.isEligibleForPipeline(target)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.invalid_target")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(inscription);
        }

        if (GearForgeData.hasTriggerAffix(target)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.already_inscribed")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(inscription);
        }

        if (!level.isClientSide) {
            boolean ok = GearForgeData.writeTriggerAffix(target, kind.id);
            if (!ok) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.failed")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(inscription);
            }

            if (!player.getAbilities().instabuild) {
                inscription.shrink(1);
            }

            level.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.trigger_inscription.success",
                            Component.translatable(kind.nameKey()).withStyle(kind.color))
                            .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.success(inscription);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("trigger_affix.transcend.tooltip.affix",
                Component.translatable(kind.nameKey()).withStyle(kind.color))
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("trigger_affix.transcend.tooltip.category",
                Component.translatable("trigger_affix.transcend.category." + kind.category.name().toLowerCase()))
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.translatable(kind.descKey())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        tooltip.add(Component.translatable("trigger_affix.transcend.tooltip.usage")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
