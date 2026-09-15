package com.mega.uom.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.fml.LogicalSide;

@Cancelable
public class ItemEntityTickEvent extends EntityEvent {
    public enum Phase {
        START, END;
    }
    public final LogicalSide side;
    public final Phase phase;
    public ItemEntityTickEvent(LogicalSide side, Phase phase, ItemEntity itemEntity) {
        super(itemEntity);
        this.side = side;
        this.phase = phase;
    }
}
