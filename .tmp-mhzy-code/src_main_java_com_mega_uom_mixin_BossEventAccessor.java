package com.mega.uom.mixin;

import java.util.UUID;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BossEvent.class})
public interface BossEventAccessor {
    @Mutable
    @Accessor("id")
    void setUUID(UUID paramUUID);
}
