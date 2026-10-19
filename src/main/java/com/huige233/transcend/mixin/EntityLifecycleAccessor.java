package com.huige233.transcend.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 暴露实体实际的世界回调，用于验证索引生命周期。 */
@Mixin(Entity.class)
public interface EntityLifecycleAccessor {
    @Accessor("levelCallback")
    EntityInLevelCallback transcend$getLevelCallback();
}
