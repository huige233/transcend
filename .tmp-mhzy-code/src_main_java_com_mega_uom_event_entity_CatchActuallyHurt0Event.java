package com.mega.uom.event.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class CatchActuallyHurt0Event extends LivingEvent {
    private final DamageSource source;
    private float amount;

    public CatchActuallyHurt0Event(LivingEntity entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Cancelable
    public static class PrePre extends CatchActuallyHurt0Event {

        public PrePre(LivingEntity entity, DamageSource source, float amount) {
            super(entity, source, amount);
        }
    }

    @Cancelable
    public static class Pre extends CatchActuallyHurt0Event {

        public Pre(LivingEntity entity, DamageSource source, float amount) {
            super(entity, source, amount);
        }
    }
}
