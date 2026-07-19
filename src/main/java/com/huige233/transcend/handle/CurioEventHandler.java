package com.huige233.transcend.handle;

import com.huige233.transcend.items.curio.AnvilCompat;
import com.huige233.transcend.items.curio.FragmentLan;
import com.huige233.transcend.items.curio.ThunderSkin;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.CuriosFinder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurioEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFragmentLanDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            ItemStack stack = CuriosFinder.findCurio(player, s -> s.getItem() instanceof FragmentLan);
            if (!stack.isEmpty() && FragmentLan.isCharged(stack)) {
                event.setCanceled(true);
                player.setHealth(5.0F);
                FragmentLan.discharge(stack);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 4, false, false));
                player.displayClientMessage(Component.translatable("FragmentLan.noDeath")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity source = event.getEntity().getKillCredit();
        if (source instanceof Player player) {
            ItemStack anvil = CuriosFinder.findCurio(player, s -> s.getItem() instanceof AnvilCompat);
            if (!anvil.isEmpty()) {
                event.setStrength(event.getStrength() * 1.5F);
                AnvilCompat item = (AnvilCompat) anvil.getItem();
                if (item.isCharged(anvil)) {
                    item.discharge(anvil);
                    event.getEntity().hurt(event.getEntity().damageSources().playerAttack(player), 25);
                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1, 1);
                }
            }
        }

        if (event.getEntity() instanceof Player player) {
            ItemStack anvil = CuriosFinder.findCurio(player, s -> s.getItem() instanceof AnvilCompat);
            if (!anvil.isEmpty()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onThunderSkinFall(LivingFallEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = CuriosFinder.findCurio(player, s -> s.getItem() instanceof ThunderSkin);
            if (!stack.isEmpty()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onThunderSkinAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack stack = CuriosFinder.findCurio(player, s -> s.getItem() instanceof ThunderSkin);
        if (!stack.isEmpty()) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(Math.min(player.getHealth() + 1, player.getMaxHealth()));
            }
            if (player.getAbsorptionAmount() < 20) {
                player.setAbsorptionAmount(player.getAbsorptionAmount() + 1);
            }
        }
    }
}
