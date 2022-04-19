package huige233.transcend.mixin;

import huige233.transcend.util.ArmorUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import morph.avaritia.handler.AvaritiaEventHandler;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(value = AvaritiaEventHandler.class,remap = false)
public abstract class MixinAvarOnDeath {


    @Inject(method ="onDeath(Lnet/minecraftforge/event/entity/living/LivingDeathEvent;)V",at = @At("HEAD"),remap = false)
    public void inject_onDeath(LivingDeathEvent event) throws Exception {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (ArmorUtils.fullEquipped(player)) {
                event.setCanceled(false);
            }
        }

    }

}