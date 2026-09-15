package com.mega.uom.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

public class MobHurtTargetEvent extends LivingEvent {
    protected Entity direct;

    public MobHurtTargetEvent(LivingEntity entity, Entity direct) {
        super(entity);
        this.direct = direct;
    }

    public Entity getDirect() {
        return direct;
    }

    @Cancelable
    public static class Pre extends MobHurtTargetEvent {
        public Pre(LivingEntity entity, Entity direct) {
            super(entity, direct);
        }
    }

    public static class Post extends MobHurtTargetEvent {
        public Post(LivingEntity entity, Entity direct) {
            super(entity, direct);
        }
    }
}
