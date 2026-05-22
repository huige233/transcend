package com.huige233.transcend.items.circle;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.circle.scroll.AncientScrollSynergy;
import com.huige233.transcend.circle.scroll.ScrollEffect;
import com.huige233.transcend.circle.scroll.ScrollEffectRegistry;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 古法咒卷 — Round 15 重构：长按持续吸魔充能后释放，整合飞升 / 法阵 / 元素专精三系统。
 *
 * <p><b>充能机制</b>：
 * <ul>
 *   <li>右键开始进入 SPYGLASS 动画，每 tick 从玩家全魔力管线吸取 mana。</li>
 *   <li>累计吸取 = {@link ScrollEffect#getManaCost()} × mastery 优惠后，释放效果并消耗一张卷轴。</li>
 *   <li>松手或魔力耗尽 → 取消（已吸取的魔力不退还）。</li>
 * </ul>
 *
 * <p><b>三系统融合</b>：
 * <ul>
 *   <li>飞升阶段门槛：每张卷轴有最低 stage 要求（基础 1 / 高阶 2 / 究极 3 / 禁咒 4）。</li>
 *   <li>元素专精共鸣：mastery 与卷轴元素匹配 → 总消耗 −30%。OMNI mastery 永远匹配。</li>
 *   <li>附近法环增幅：响应半径内活跃法环按 tier 提升吸取速率（×1.2 ~ ×2.0）。</li>
 *   <li>飞升等级加速：每级飞升 +2 mana/tick 基础吸取速率。</li>
 * </ul>
 */
public class AncientSpellScrollItem extends Item {

    private static final int COOLDOWN_TICKS = 60;
    /** NBT 键：累计已吸取魔力（充能进度） */
    private static final String TAG_CHARGE = "transcend_charge";

    private final String scrollType;

    public AncientSpellScrollItem(String scrollType) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.scrollType = scrollType;
        ModItems.ITEMS.add(this);
    }

    public String getScrollType() {
        return scrollType;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // ── 长按充能机制 ──

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 足够长以让任何充能完成（实际由 mana / drain rate 决定真实时长）
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 服务端：阶段门检查
        if (!level.isClientSide) {
            int requiredStage = AncientScrollSynergy.getRequiredStage(scrollType);
            PlayerAscensionData data = AscensionCapability.get(player);
            if (data.getStage() < requiredStage && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("scroll.transcend.stage_gate", requiredStage)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            // 重置进度
            stack.getOrCreateTag().putInt(TAG_CHARGE, 0);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        ScrollEffect effect = ScrollEffectRegistry.get(scrollType);
        if (effect == null) {
            player.releaseUsingItem();
            return;
        }

        // 总消耗 = 原始 × mastery 优惠
        int totalCost = AncientScrollSynergy.getEffectiveCost(player, scrollType, effect.getManaCost());

        // 每 tick drain
        int drainPerTick = AncientScrollSynergy.getDrainPerTick(player, level);

        // 创造模式 → 跳过 mana 检查 / 消耗
        int actualDrain;
        if (player.getAbilities().instabuild) {
            actualDrain = drainPerTick;
        } else {
            int currentMana = MagicCrystalHelper.countMana(player);
            actualDrain = Math.min(drainPerTick, currentMana);
            if (actualDrain == 0) {
                // 魔力耗尽 — 中断
                player.displayClientMessage(
                        Component.translatable("scroll.transcend.charge_interrupted")
                                .withStyle(ChatFormatting.RED), true);
                stack.getOrCreateTag().remove(TAG_CHARGE);
                player.releaseUsingItem();
                return;
            }
            MagicCrystalHelper.consumeMana(player, actualDrain);
        }

        // 累计进度
        CompoundTag tag = stack.getOrCreateTag();
        int progress = tag.getInt(TAG_CHARGE) + actualDrain;

        if (progress >= totalCost) {
            // 完成 — 释放效果
            boolean ok = effect.execute(serverLevel, player, player.blockPosition());

            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.5F, 0.8F);

            if (ok && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            tag.remove(TAG_CHARGE);
            player.releaseUsingItem();
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        } else {
            tag.putInt(TAG_CHARGE, progress);
            // 充能音效（每 10 tick 一次脉冲）
            if (level.getGameTime() % 10 == 0) {
                float pitch = 1.0F + (float) progress / totalCost; // 升调
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.35F, pitch);
            }
            // Round 18: 充能视觉粒子（每 5 tick 一圈，元素配色）
            if (level.getGameTime() % 5 == 0) {
                emitChannelParticles(serverLevel, player, scrollType);
            }
        }
    }

    /**
     * Round 18: 围绕施法者生成元素色的充能粒子环。颜色取自 scroll 绑定的 SpellElement（OMNI 卷轴 → 白）。
     */
    private static void emitChannelParticles(net.minecraft.server.level.ServerLevel level,
                                              net.minecraft.server.level.ServerPlayer player,
                                              String scrollType) {
        com.huige233.transcend.spell.SpellElement scrollElement =
                com.huige233.transcend.circle.scroll.AncientScrollSynergy.getScrollElement(scrollType);
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (scrollElement != null) {
            r = scrollElement.getParticleR();
            g = scrollElement.getParticleG();
            b = scrollElement.getParticleB();
        }
        net.minecraft.core.particles.DustParticleOptions dust =
                new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(r, g, b), 1.4F);

        long t = level.getGameTime();
        // 8 个粒子环绕，慢速旋转 + 上下浮动
        for (int i = 0; i < 8; i++) {
            double angle = (t / 5.0 + i * (Math.PI / 4.0)) % (2 * Math.PI);
            double radius = 1.0 + 0.3 * Math.sin(t / 10.0);
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.8 + Math.sin(t / 8.0 + i) * 0.4;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(dust, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        // 玩家松手 — 清除进度（已吸取的魔力不退）
        if (!level.isClientSide && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_CHARGE)) {
                int progress = tag.getInt(TAG_CHARGE);
                tag.remove(TAG_CHARGE);
                if (progress > 0 && entity instanceof Player p) {
                    p.displayClientMessage(
                            Component.translatable("scroll.transcend.charge_cancelled", progress)
                                    .withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }

    // ── Tooltip ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        // 卷轴描述与风味文本（保留旧 lang 键）
        tooltip.add(Component.translatable("scroll.transcend." + scrollType + ".desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("scroll.transcend." + scrollType + ".flavor")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        tooltip.add(Component.empty());

        // Stage 要求
        int reqStage = AncientScrollSynergy.getRequiredStage(scrollType);
        if (reqStage > 0) {
            tooltip.add(Component.translatable("scroll.transcend.stage_required", reqStage)
                    .withStyle(ChatFormatting.GOLD));
        }

        // 元素共鸣提示
        SpellElement element = AncientScrollSynergy.getScrollElement(scrollType);
        if (element != null) {
            tooltip.add(Component.translatable("scroll.transcend.element_resonance",
                            Component.translatable(element.getDisplayKey()))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("scroll.transcend.omni_scroll")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // 总消耗显示
        ScrollEffect effect = ScrollEffectRegistry.get(scrollType);
        if (effect != null) {
            tooltip.add(Component.translatable("scroll.transcend.total_cost", effect.getManaCost())
                    .withStyle(ChatFormatting.BLUE));
        }

        // 充能中的进度
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_CHARGE) && effect != null) {
                int charge = tag.getInt(TAG_CHARGE);
                int total = effect.getManaCost();
                if (charge > 0) {
                    int pct = (int) (100.0 * charge / total);
                    tooltip.add(Component.translatable("scroll.transcend.charging_progress",
                                    charge, total, pct)
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
        }

        tooltip.add(Component.translatable("scroll.transcend.hold_to_channel")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
