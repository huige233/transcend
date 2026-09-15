package com.mega.uom.mixin;

import com.mega.uom.event.ClientProgramTickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeEventFactory.class, remap = false)
public class ForgeEventFactoryMixin {
    @Inject(method = "onPreClientTick", at = @At("HEAD"))
    private static void onPreClientTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new ClientProgramTickEvent(TickEvent.Phase.START));
    }

    @Inject(method = "onPostClientTick", at = @At("HEAD"))
    private static void onPostClientTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new ClientProgramTickEvent(TickEvent.Phase.END));
    }
}
