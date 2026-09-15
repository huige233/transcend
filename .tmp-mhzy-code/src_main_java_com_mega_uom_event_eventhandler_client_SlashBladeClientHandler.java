package com.mega.uom.event.eventhandler.client;

import com.mega.uom.common.data.FantasyEndingBuiltInRegistry;
import com.mega.uom.common.items.combat.KillsCountItem;
import com.mega.uom.common.register.TargetRegister;
import com.mega.uom.compat.slash_blade.SBItemHandler;
import com.mega.uom.compat.SafeClass;
import mods.flammpfeil.slashblade.SlashBladeCreativeGroup;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SlashBladeClientHandler {
    @SubscribeEvent
    public static void doClientStuff(FMLClientSetupEvent event) {
        if (SafeClass.isSlahbladeReLoaded()) {
            ItemProperties.register(SBItemHandler.getItem(SBItemHandler.FE_BLADE), new ResourceLocation("slashblade:user"), new ClampedItemPropertyFunction() {
                public float unclampedCall(ItemStack p_174564_, @Nullable ClientLevel p_174565_, @Nullable LivingEntity p_174566_, int p_174567_) {
                    BladeModel.user = p_174566_;
                    return 0.0F;
                }
            });
        }
    }

    @SubscribeEvent
    public static void Baked(ModelEvent.ModifyBakingResult event) {
        if (SafeClass.isSlahbladeReLoaded()) {
            mods.flammpfeil.slashblade.client.ClientHandler.bakeBlade(SBItemHandler.getItem(SBItemHandler.FE_BLADE), event);
        }
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (SafeClass.isSlahbladeReLoaded()) {
            if (event.getTabKey() == SlashBladeCreativeGroup.SLASHBLADE_GROUP.getKey())
                event.accept(FantasyEndingBuiltInRegistry.FE_BLADE_DEFINE.getBlade());
            if (event.getTabKey() == TargetRegister.TAB2.getKey()) {
                ItemStack stack = FantasyEndingBuiltInRegistry.FE_BLADE_DEFINE.getBlade();
                ((KillsCountItem) stack.getItem()).addKillsCount(stack, 232424314);
                event.accept(stack);
            }
        }
    }
}
