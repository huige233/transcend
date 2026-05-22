package com.huige233.transcend.handle;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.network.S2CTotemPack;
import com.huige233.transcend.util.CuriosFinder;
import com.huige233.transcend.util.TextUtils;
import com.huige233.transcend.util.TranscendUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModHandle {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof TranscendSword)) return;

        String infinity = TextUtils.makeFabulous(I18n.get("tip.transcend"));

        for (int x = 0; x < event.getToolTip().size(); ++x) {
            String line = event.getToolTip().get(x).getString();

            if (line.contains(I18n.get("attribute.name.generic.attack_damage")) || line.contains("Attack Damage")) {
                event.getToolTip().set(x, Component.literal("+")
                        .withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(infinity))
                        .append(" ")
                        .append(Component.translatable("attribute.name.generic.attack_damage")
                                .withStyle(ChatFormatting.BLUE)));
            } else if (line.contains(I18n.get("attribute.name.transcend.transcend_damage")) || line.contains("Transcend Damage")) {
                event.getToolTip().set(x, Component.literal("+")
                        .withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(infinity))
                        .append(" ")
                        .append(Component.translatable("attribute.name.transcend.transcend_damage")
                                .withStyle(ChatFormatting.BLUE)));
            } else if (line.contains("Entity Reach") || line.contains("entity_reach") || line.contains(I18n.get("forge.entity_reach"))) {
                event.getToolTip().set(x, Component.literal("+")
                        .withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(infinity))
                        .append(" ")
                        .append(Component.literal(I18n.get("forge.entity_reach"))
                                .withStyle(ChatFormatting.BLUE)));
            } else if (line.contains(I18n.get("attribute.name.generic.attack_speed")) || line.contains("Attack Speed")) {
                event.getToolTip().set(x, Component.literal("+")
                        .withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(infinity))
                        .append(" ")
                        .append(Component.translatable("attribute.name.generic.attack_speed")
                                .withStyle(ChatFormatting.BLUE)));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if(CuriosFinder.hasCurio(player, ModItems.transcend_curio.get())){
                event.setCanceled(true);
                player.setInvulnerable(true);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTotemDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            ItemStack totem = TranscendUtil.getPlayerTotemItem(player);
            if(!totem.isEmpty()){
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CTotemPack(totem, player.getId()));

                player.removeAllEffects();
                if(totem.getDamageValue() == totem.getMaxDamage() - 1)player.displayClientMessage(Component.translatable("tooltip.transcend.totem_break"), false);
                player.setHealth(player.getMaxHealth());
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2600, 4));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 700, 2));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1100, 0));
                totem.hurtAndBreak(1, player, e -> e.swing(InteractionHand.MAIN_HAND));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if(CuriosFinder.hasCurio(player, ModItems.transcend_curio.get())){
                event.setCanceled(true);
                player.setInvulnerable(true);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttacked(LivingAttackEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if(CuriosFinder.hasCurio(player, ModItems.transcend_curio.get())){
                player.setInvulnerable(true);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if(CuriosFinder.hasCurio(player, ModItems.transcend_curio.get())){
                player.setInvulnerable(true);
                event.setAmount(0.0f);
                player.hurtTime = 0;
                player.deathTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingKnockBack(LivingKnockBackEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if(CuriosFinder.hasCurio(player, ModItems.transcend_curio.get())){
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobChangeTarget(LivingChangeTargetEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            if(player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if(CuriosFinder.hasCurio(player, ModItems.transcend_curio.get())){
                event.setCanceled(true);
            }
        }
    }
}
