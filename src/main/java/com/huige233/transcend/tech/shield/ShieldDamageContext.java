package com.huige233.transcend.tech.shield;

import com.huige233.transcend.entity.TestDummy.DamageCategory;

import javax.annotation.Nullable;


/** 封装一次护盾结算所需的伤害类别、数额、绕盾标记和穿透强度。 */
public record ShieldDamageContext(@Nullable DamageCategory category, float amount, boolean bypassesShield,
                                  int penetrationStrength) {
    public ShieldDamageContext {
        amount = Math.max(0.0F, amount);
        penetrationStrength = Math.max(0, penetrationStrength);
    }

    public ShieldDamageContext(@Nullable DamageCategory category, float amount, boolean bypassesShield) {
        this(category, amount, bypassesShield, 0);
    }

    public int categoryIndex() {
        return category == null ? -1 : category.index;
    }
}
