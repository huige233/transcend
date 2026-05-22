package com.huige233.transcend.mixin;


import com.huige233.transcend.util.TranscendUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity{

    @Unique
    private TranscendUtil util = new TranscendUtil((Player) (Object) this);

    public PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_){
        super(p_20966_,p_20967_);
    }

    @Inject(method = "tick",at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        util.tick();
    }
}
