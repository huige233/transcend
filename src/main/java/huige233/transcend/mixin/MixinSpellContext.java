package huige233.transcend.mixin;

import huige233.transcend.util.ArmorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.SpellRuntimeException;

@Mixin(SpellContext.class)
public abstract class MixinSpellContext {


    @Inject(method = "verifyEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    public void inject_verifyEntity(Entity e, CallbackInfo ci) throws SpellRuntimeException {
        if(e instanceof EntityPlayer) {
            if(ArmorUtils.fullEquipped((EntityPlayer) e)) {
                throw new SpellRuntimeException(SpellRuntimeException.IMMUNE_TARGET);
            }
        }
    }

}
