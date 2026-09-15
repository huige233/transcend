package com.huige233.transcend.tech.ammo;

   
                                     
   
/** 封装弹体的穿透数量、溅射半径和爆炸标记，并提供常用行为组合。 */
public final class BoltBehavior {

    
    public final int pierce;
    
    public final float splashRadius;
    
    public final boolean explode;

    public BoltBehavior(int pierce, float splashRadius, boolean explode) {
        this.pierce = pierce;
        this.splashRadius = splashRadius;
        this.explode = explode;
    }

    
    public static BoltBehavior direct() {
        return new BoltBehavior(0, 0.0F, false);
    }

    
    public static BoltBehavior splash(float radius) {
        return new BoltBehavior(0, radius, false);
    }

    
    public static BoltBehavior piercing() {
        return new BoltBehavior(2, 0.0F, false);
    }

    
    public static BoltBehavior explosive() {
        return new BoltBehavior(0, 0.0F, true);
    }
}
