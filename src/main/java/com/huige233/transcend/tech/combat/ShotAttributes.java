package com.huige233.transcend.tech.combat;

import com.huige233.transcend.tech.attribute.ModifierOp;
import com.huige233.transcend.tech.attribute.TechAttrModifier;
import com.huige233.transcend.tech.attribute.TechAttribute;

import java.util.List;


/** 兼容旧枪械基础伤害配置，并按加法与乘法修饰器计算单次射击的基础伤害。 */
public final class ShotAttributes {
    private ShotAttributes() {}

    public static float damageBase(double configuredGunBase, double attributeBase, List<TechAttrModifier> modifiers) {
        double base = attributeBase == TechAttribute.DAMAGE_BASE.defaultValue ? configuredGunBase : attributeBase;
        double add = 0.0D, mult = 1.0D;
        for (TechAttrModifier modifier : modifiers) {
            if (!Double.isFinite(modifier.amount())) continue;
            if (modifier.op() == ModifierOp.ADD) add += modifier.amount();
            else mult *= modifier.amount();
        }
        return CombatNumbers.nonNegative((base + add) * mult);
    }
}
