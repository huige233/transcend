package com.mega.uom.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class AttackEntityEvent extends PlayerEvent {
    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        super(player);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }

    @Cancelable
    public static class Pre extends AttackEntityEvent {

        public Pre(Player player, Entity target) {
            super(player, target);
        }
    }

    public static class Post extends AttackEntityEvent {

        public Post(Player player, Entity target) {
            super(player, target);
        }
    }
}