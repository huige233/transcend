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

/**
 * R91: 触发型词条刻印 — 玩家手持本物品，把要铭刻的装备放副手，shift+右键空气铭刻。
 *
 * <h2>交互流程</h2>
 * <ol>
 *   <li>主手：本刻印物品（带 {@link TriggerAffixKind}）</li>
 *   <li>副手：要铭刻的装备（必须是 {@link GearForgeData#isEligibleForPipeline 合格}，且未铭刻过 trigger affix）</li>
 *   <li>shift + 右键空气 → 铭刻成功，消耗 1 刻印 + 副手装备永久获得该触发词条</li>
 * </ol>
 *
 * <h2>不可逆 / 限制</h2>
 * <ul>
 *   <li>每件装备最多 1 个触发词条 — 第二次铭刻同件装备失败</li>
 *   <li>不需要 CRUCIBLE 前置（白板 isDamageableItem 装备也可铭刻）— 与 5 阶段独立</li>
 *   <li>不可逆 — 铭刻后无法替换/移除（除非装备销毁）</li>
 * </ul>
 *
 * <h2>不消耗当 fail</h2>
 * <p>如果副手为空 / 副手装备不合格 / 副手已有 trigger affix → 显示提示，**不消耗主手刻印**。
 */
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

        // 必须 sneak（避免误用）
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.shift_hint")
                                .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.pass(inscription);
        }

        // 副手必须有合格装备
        if (target.isEmpty() || !GearForgeData.isEligibleForPipeline(target)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.invalid_target")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(inscription);
        }

        // 副手装备不能已铭刻 trigger affix
        if (GearForgeData.hasTriggerAffix(target)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.already_inscribed")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(inscription);
        }

        // 服务端铭刻
        if (!level.isClientSide) {
            boolean ok = GearForgeData.writeTriggerAffix(target, kind.id);
            if (!ok) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.trigger_inscription.failed")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(inscription);
            }

            // 消耗 1 刻印
            if (!player.getAbilities().instabuild) {
                inscription.shrink(1);
            }

            // 反馈
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

        // 词条名（带主题色）
        tooltip.add(Component.translatable("trigger_affix.transcend.tooltip.affix",
                Component.translatable(kind.nameKey()).withStyle(kind.color))
                .withStyle(ChatFormatting.GRAY));

        // 类别（ON_KILL/ON_HURT/PERIODIC）
        tooltip.add(Component.translatable("trigger_affix.transcend.tooltip.category",
                Component.translatable("trigger_affix.transcend.category." + kind.category.name().toLowerCase()))
                .withStyle(ChatFormatting.DARK_GRAY));

        // 效果简介
        tooltip.add(Component.translatable(kind.descKey())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        // 用法提示
        tooltip.add(Component.translatable("trigger_affix.transcend.tooltip.usage")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
