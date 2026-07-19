package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.ForgeStage;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExperienceAwakeningHandler {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide) return;
        ItemStack weapon = killer.getItemInHand(InteractionHand.MAIN_HAND);
        if (!eligibleForAwakening(weapon, GearCategory.WEAPON)) return;

        GearForgeData.incrementExperience(weapon, 1, 0, 0, 0);
        tryAwaken(killer, weapon);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player breaker = event.getPlayer();
        if (breaker == null || breaker.level().isClientSide) return;
        ItemStack tool = breaker.getItemInHand(InteractionHand.MAIN_HAND);
        if (!eligibleForAwakening(tool, GearCategory.TOOL)) return;

        GearForgeData.incrementExperience(tool, 0, 0, 1, 0);
        tryAwaken(breaker, tool);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (event.getAmount() <= 0) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (!eligibleForAwakening(armor, GearCategory.ARMOR)) continue;
            GearForgeData.incrementExperience(armor, 0, 0, 0, 1);
            tryAwaken(player, armor);
        }
    }

    public static void noteCast(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;
        if (stack.isEmpty() || !GearForgeData.isStageWritten(stack, ForgeStage.CRUCIBLE)) return;
        GearForgeData.incrementExperience(stack, 0, 1, 0, 0);

    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {

    }

    private static boolean eligibleForAwakening(ItemStack stack, GearCategory expected) {
        if (stack.isEmpty()) return false;
        if (!GearForgeData.isEligibleForPipeline(stack)) return false;
        if (!GearForgeData.isStageWritten(stack, ForgeStage.CRUCIBLE)) return false;
        return GearCategory.classify(stack) == expected;
    }

    private static void tryAwaken(Player player, ItemStack stack) {
        int oldTier = GearForgeData.getExperience(stack).tier();
        int newTier = GearForgeData.upgradeExperienceTier(stack);
        if (newTier > oldTier && player instanceof ServerPlayer sp) {

            sp.displayClientMessage(
                    Component.translatable("msg.transcend.experience.awakened",
                            stack.getHoverName(), newTier, GearForgeData.MAX_EXPERIENCE_TIER)
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }
}
