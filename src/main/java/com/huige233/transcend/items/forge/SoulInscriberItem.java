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

public class SoulInscriberItem extends Item {

    public static final String TAG_CAPTURED = "transcend_captured_mob";
    public static final long SOUL_ENERGY_COST = 50L;
    public static final int ECHO_TIER = 1;
    public static final String ECHO_TYPE_KILL = "kill";

    public SoulInscriberItem() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

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

    public static boolean tryCapture(ItemStack inscriber, LivingEntity victim) {
        if (inscriber.isEmpty() || !(inscriber.getItem() instanceof SoulInscriberItem)) return false;
        if (hasCaptured(inscriber)) return false;
        EntityType<?> type = victim.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return false;
        setCapturedMobId(inscriber, id.toString());
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack inscriber = player.getItemInHand(hand);

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
        if (data.getRitualTier() < 1) {
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

        if (!data.consumeSoulEnergy(SOUL_ENERGY_COST)) {
            return InteractionResultHolder.consume(inscriber);
        }
        boolean ok = GearForgeData.addSoulEcho(gear, capturedId, ECHO_TYPE_KILL, ECHO_TIER);
        if (!ok) {

            data.addSoulEnergy(SOUL_ENERGY_COST);
            return InteractionResultHolder.consume(inscriber);
        }
        setCapturedMobId(inscriber, null);

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
