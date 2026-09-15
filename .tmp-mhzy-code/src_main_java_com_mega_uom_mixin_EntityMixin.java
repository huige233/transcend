package com.mega.uom.mixin;

import com.mega.uom.event.entity.EntityHurtEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract boolean equals(Object p_20245_);

    @Shadow
    public abstract Level level();

    @Shadow private Level level;

    @Shadow protected abstract Vec3 collide(Vec3 p_20273_);

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void hurt(DamageSource p_19946_, float p_19947_, CallbackInfoReturnable<Boolean> cir) {
        EntityHurtEvent event = new EntityHurtEvent((Entity) (Object) this, p_19946_, p_19947_);
        if (MinecraftForge.EVENT_BUS.post(event))
            cir.setReturnValue(false);
    }
}
