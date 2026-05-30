package com.huige233.transcend.items.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.gear.ForgeStage;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * R84: 灵魂铭刻笔（Soul Inscriber）— 造物之道 A 阶段（灵魂注魂）的唯一交互入口。
 *
 * <h2>使用流程</h2>
 * <ol>
 *   <li><b>捕获</b>：玩家手持本物品击杀任意 LivingEntity → mobId 写入物品 NBT</li>
 *   <li><b>铭刻</b>：玩家主手持本物品（已捕获）+ 副手持装备 → 右键 → 写入 SoulEcho 到装备 NBT；
 *       消耗 {@value #SOUL_ENERGY_COST} 灵魂能；清空物品 NBT</li>
 * </ol>
 *
 * <h2>NBT 结构</h2>
 * <pre>
 * "transcend:captured_mob": "minecraft:zombie"   // 已捕获的目标 mob id（空则未捕获）
 * </pre>
 *
 * <h2>R80 不可逆门</h2>
 * 装备必须已写入 CRUCIBLE（E 必先）；上限 3 SoulEcho（{@link GearForgeData#MAX_SOUL_ECHOES}）；
 * SoulEcho list append-only（不可移除）。
 *
 * <h2>SoulEnergy 整合（R77）</h2>
 * 每次铭刻消耗 {@value #SOUL_ENERGY_COST} 灵魂能（玩家飞升阶段 ≥ 1 才有灵魂能容量）；
 * 不足则铭刻失败，物品不消耗、NBT 保留。
 */
public class SoulInscriberItem extends Item {

    public static final String TAG_CAPTURED = "transcend_captured_mob";
    public static final long SOUL_ENERGY_COST = 50L;
    public static final int ECHO_TIER = 1;
    public static final String ECHO_TYPE_KILL = "kill";

    public SoulInscriberItem() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    // ─── NBT 助手 ────────────────────────────────────────────────────

    @Nullable
    public static String getCapturedMobId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CAPTURED)) return null;
        String id = tag.getString(TAG_CAPTURED);
        return id.isEmpty() ? null : id;
    }

    public static void setCapturedMobId(ItemStack stack, @Nullable String mobId) {
        if (stack.isEmpty()) return;
        if (mobId == null || mobId.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) tag.remove(TAG_CAPTURED);
        } else {
            stack.getOrCreateTag().putString(TAG_CAPTURED, mobId);
        }
    }

    public static boolean hasCaptured(ItemStack stack) {
        return getCapturedMobId(stack) != null;
    }

    // ─── 捕获：被击杀回调（由 LivingDeathEvent 调用）──────────────────

    /**
     * 由 {@link com.huige233.transcend.gear.forge.SoulInscribeHandler#onLivingDeath} 调用。
     * 仅当 inscriber 当前未捕获时写入 mobId。
     */
    public static boolean tryCapture(ItemStack inscriber, LivingEntity victim) {
        if (inscriber.isEmpty() || !(inscriber.getItem() instanceof SoulInscriberItem)) return false;
        if (hasCaptured(inscriber)) return false;
        EntityType<?> type = victim.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return false;
        setCapturedMobId(inscriber, id.toString());
        return true;
    }

    // ─── 铭刻：右键交互 ───────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack inscriber = player.getItemInHand(hand);
        // 仅在主手时尝试铭刻
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(inscriber);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(inscriber);
        }
        String capturedId = getCapturedMobId(inscriber);
        if (capturedId == null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_inscriber.empty")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.consume(inscriber);
        }

        ItemStack gear = player.getOffhandItem();
        if (gear.isEmpty() || !GearForgeData.isEligibleForPipeline(gear)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_inscriber.no_gear")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.consume(inscriber);
        }
        if (!GearForgeData.isStageWritten(gear, ForgeStage.CRUCIBLE)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_inscriber.no_crucible")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.consume(inscriber);
        }
        if (GearForgeData.getSoulEchoes(gear).size() >= GearForgeData.MAX_SOUL_ECHOES) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_inscriber.echoes_full",
                            GearForgeData.MAX_SOUL_ECHOES)
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.consume(inscriber);
        }

        PlayerAscensionData data = AscensionCapability.get(player);
        if (data == null) {
            return InteractionResultHolder.consume(inscriber);
        }
        if (data.getStage() < 1) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_inscriber.stage_locked")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.consume(inscriber);
        }
        if (data.getSoulEnergy() < SOUL_ENERGY_COST) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_inscriber.no_soul_energy",
                            SOUL_ENERGY_COST, data.getSoulEnergy())
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.consume(inscriber);
        }

        // 全部校验通过：扣灵魂能，写 echo，清 NBT
        if (!data.consumeSoulEnergy(SOUL_ENERGY_COST)) {
            return InteractionResultHolder.consume(inscriber);
        }
        boolean ok = GearForgeData.addSoulEcho(gear, capturedId, ECHO_TYPE_KILL, ECHO_TIER);
        if (!ok) {
            // 不应发生（已校验过）；回滚灵魂能
            data.addSoulEnergy(SOUL_ENERGY_COST);
            return InteractionResultHolder.consume(inscriber);
        }
        setCapturedMobId(inscriber, null);

        // 反馈
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.9F, 1.3F);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE,
                SoundSource.PLAYERS, 0.6F, 0.8F);
        int sockets = GearForgeData.getSoulEchoes(gear).size();
        Component mobName = mobDisplayName(capturedId);
        player.displayClientMessage(
                Component.translatable("msg.transcend.soul_inscriber.inscribed",
                        mobName, sockets, GearForgeData.MAX_SOUL_ECHOES)
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        return InteractionResultHolder.consume(inscriber);
    }

    /** Sneak + rclick on entity: 释放当前 captured 的 NBT（玩家想换目标时用）。 */
    @Override
    public InteractionResult interactLivingEntity(ItemStack inscriber, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!hasCaptured(inscriber)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        setCapturedMobId(inscriber, null);
        player.displayClientMessage(
                Component.translatable("msg.transcend.soul_inscriber.released")
                        .withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }

    // ─── Tooltip ────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String capturedId = getCapturedMobId(stack);
        if (capturedId == null) {
            tooltip.add(Component.translatable("tooltip.transcend.soul_inscriber.empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("tooltip.transcend.soul_inscriber.captured", mobDisplayName(capturedId))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("tooltip.transcend.soul_inscriber.usage_capture")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.soul_inscriber.usage_inscribe",
                SOUL_ENERGY_COST)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.soul_inscriber.usage_release")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** mob id "minecraft:zombie" → Component（优先使用 entity_type.<ns>.<path> lang key）。 */
    private static Component mobDisplayName(String mobId) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(mobId);
            if (rl == null) return Component.literal(mobId);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (type != null) return type.getDescription();
        } catch (Exception e) {
            Transcend.LOGGER.debug("Failed to resolve mob name for {}", mobId, e);
        }
        return Component.literal(mobId);
    }
}
