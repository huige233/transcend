package huige233.transcend.mixin;

import morph.avaritia.handler.AvaritiaEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static morph.avaritia.handler.AvaritiaEventHandler.isInfinite;

@Mixin(value = AvaritiaEventHandler.class,remap = false)
public abstract class MixinAvarOnDeath {


    @Inject(method ="onDeath(Lnet/minecraftforge/event/entity/living/LivingDeathEvent;)V",at = @At("HEAD"),remap = false)
    public void inject_onDeath(LivingDeathEvent event, CallbackInfo ci) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (isInfinite(player) && !event.getSource().getDamageType().equals("infinity")) {
                event.setCanceled(false);
            }
        }
    }
}