package com.huige233.transcend.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
/** Boss 血条存取器接口(mixin)。 */
public interface BossHealthOverlayAccessor {

    @Accessor("events")
    Map<UUID, ?> transcend$getEvents();
}
