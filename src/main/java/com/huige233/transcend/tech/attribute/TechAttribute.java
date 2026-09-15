package com.huige233.transcend.tech.attribute;

   
                         
  
                                    
                                        
  
                                                     
   
/** 列举科技装备的战斗、弹药、热量和护盾属性，并定义标识、默认值与默认运算方式。 */
public enum TechAttribute {

    
    
    DAMAGE_BASE("damage_base", 4.0, ModifierOp.ADD),
    
    DAMAGE_MULT("damage_mult", 1.0, ModifierOp.MULT),
    
    CRIT_CHANCE("crit_chance", 0.0, ModifierOp.ADD),
    
    CRIT_MULT("crit_mult", 1.5, ModifierOp.MULT),
    
    ARMOR_PIERCE("armor_pierce", 0.0, ModifierOp.ADD),
    
    BOSS_PENETRATION("boss_penetration", 0.0, ModifierOp.ADD),
    
    BOSS_DAMAGE_PERCENT("boss_damage_percent", 0.0, ModifierOp.ADD),

    
    
    BOLT_SPEED("bolt_speed", 1.0, ModifierOp.MULT),
    
    CHARGE_TICKS("charge_ticks", 30.0, ModifierOp.MULT),
    
    SHOT_COUNT("shot_count", 1.0, ModifierOp.ADD),
    
    ACCURACY_SPREAD("spread", 0.0, ModifierOp.ADD),

    
    
    AMMO_CAPACITY("ammo_capacity", 12.0, ModifierOp.ADD),
    
    ENERGY_COST("energy_cost", 1.0, ModifierOp.MULT),
    
    REGEN_RATE("regen_rate", 0.25, ModifierOp.ADD),

    
    
    HEAT_PER_SHOT("heat_per_shot", 1.0, ModifierOp.MULT),
    
    HEAT_CAPACITY("heat_capacity", 100.0, ModifierOp.ADD),
    
    COOLING_RATE("cooling_rate", 0.5, ModifierOp.ADD),
    
    OVERHEAT_PENALTY("overheat_penalty", 1.0, ModifierOp.MULT),

    
    
    SHIELD_MAX("shield_max", 200.0, ModifierOp.ADD),
    
    SHIELD_REGEN("shield_regen", 0.4, ModifierOp.ADD),
    
    SHIELD_EFFICIENCY("shield_efficiency", 1.0, ModifierOp.MULT),
    
    SHIELD_CATEGORY_FOCUS("shield_category_focus", -1.0, ModifierOp.ADD),
    
    SHIELD_FOCUS_RATIO("shield_focus_ratio", 3.0, ModifierOp.ADD),
    
    SHIELD_LEAK_MULT("shield_leak_mult", 0.25, ModifierOp.MULT),

    
    SHIELD_PENETRATION("shield_penetration", 0.0, ModifierOp.ADD),

    
    
    PIERCE_COUNT("pierce_count", 0.0, ModifierOp.ADD),
    
    SPLASH_RADIUS("splash_radius", 0.0, ModifierOp.ADD),
    
    FE_EFFICIENCY("fe_efficiency", 1.0, ModifierOp.MULT),
    
    SILENCED("silenced", 0.0, ModifierOp.ADD),
    
    SELF_SHIELD_REQ("self_shield_req", 0.0, ModifierOp.ADD);

    public final String id;
    
    public final double defaultValue;
    
    public final ModifierOp defaultOp;

    TechAttribute(String id, double defaultValue, ModifierOp defaultOp) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.defaultOp = defaultOp;
    }
}
