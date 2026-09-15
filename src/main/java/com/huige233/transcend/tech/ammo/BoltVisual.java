package com.huige233.transcend.tech.ammo;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

   
                                             
   
/** 承载弹体显示名称、颜色、拖尾粒子、渲染尺寸和发光标记。 */
public final class BoltVisual {

    
    public final Component displayName;
    
    public final int color;
    
    @Nullable
    public final ParticleOptions trail;
    
    public final float size;
    
    public final boolean glowing;

    public BoltVisual(Component displayName, int color, @Nullable ParticleOptions trail, float size, boolean glowing) {
        this.displayName = displayName;
        this.color = color;
        this.trail = trail;
        this.size = size;
        this.glowing = glowing;
    }
}
