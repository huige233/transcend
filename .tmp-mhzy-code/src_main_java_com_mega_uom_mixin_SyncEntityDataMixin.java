package com.mega.uom.mixin;

import com.mega.uom.common.register.ModMobEffects;
import com.mega.uom.util.data.LivingEntityExpandedContext;
import com.mega.uom.util.entity.EntityActuallyHurt;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@SuppressWarnings("unchecked")
@Mixin(SynchedEntityData.class)
public class SyncEntityDataMixin {
    @Shadow
    @Final
    private static Object2IntMap<Class<? extends Entity>> ENTITY_ID_POOL;
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private Entity entity;
    @Shadow
    @Final
    private Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById;

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("HEAD"), cancellable = true)
    private <T> void set(EntityDataAccessor<T> p_276368_, T p_276363_, boolean p_276370_, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            MobEffect banHealing = ModMobEffects.BAN_HEALING.get();
            if (!living.isRemoved() && ((LivingEntityAccessor) living).activeEffects() != null && living.hasEffect(banHealing)) {
                EntityActuallyHurt.IndexAndType indexAndType = LivingEntityExpandedContext.getIndexAndType(living);
                if (indexAndType == null) {
                    Map<MobEffect, MobEffectInstance> effects = living.getActiveEffectsMap();
                    MobEffectInstance effectInstance = living.getEffect(banHealing);
                    effects.remove(banHealing);
                    LivingEntityExpandedContext.reload(living);
                    effects.put(banHealing, effectInstance);

                    return;
                }
                if (indexAndType == EntityActuallyHurt.NULL_DATA) return;
                if (p_276368_.getId() == indexAndType.index()) {
                    if (indexAndType.isFloat()) {
                        if ((float) p_276363_ > living.getHealth()) {
                            p_276363_ = (T) (Object) (((float) p_276363_ - living.getHealth()) *
                                    Mth.clamp((1.0F - (living.getEffect(ModMobEffects.BAN_HEALING.get()).getAmplifier() + 1) * 0.1F), 0F, 1F)
                                    + living.getHealth());
                            EntityActuallyHurt.set((SyncEntityDataAccessor) this, p_276368_, p_276363_);
                            ci.cancel();
                        }
                    } else {
                        if ((double) p_276363_ > living.getHealth()) {
                            p_276363_ = (T) (Object) ((
                                    (double) p_276363_ - living.getHealth()) *
                                    Mth.clamp((1.0D - (living.getEffect(ModMobEffects.BAN_HEALING.get()).getAmplifier() + 1) * 0.1D), 0D, 1D)
                                    + living.getHealth());
                            EntityActuallyHurt.set((SyncEntityDataAccessor) this, p_276368_, p_276363_);
                            ci.cancel();
                        }
                    }
                }

            }
        }
    }
}
