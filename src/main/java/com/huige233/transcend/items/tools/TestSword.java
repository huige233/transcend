package com.huige233.transcend.items.tools;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.ModToolTiers;
import com.huige233.transcend.TranscendDamage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.UUID;

public class TestSword extends SwordItem {
    public TestSword(){
        super(ModToolTiers.NORMAL,0,0f,(new Properties()).rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    private static final UUID MAX_HEALTH_ID = UUID.fromString("ae3f67b9-08e3-4866-8644-53770179117a");

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide) {
            if(target instanceof LivingEntity pvp){
                Attribute attribute = Attributes.MAX_HEALTH;
                AttributeInstance attributeInstance = pvp.getAttribute(attribute);

                if (attributeInstance != null) {
                    AttributeModifier srcModifier = attributeInstance.getModifier(MAX_HEALTH_ID);
                    if (srcModifier == null) {
                        attributeInstance.addTransientModifier(new AttributeModifier(MAX_HEALTH_ID, "Weapon Modifier", -1.01f, AttributeModifier.Operation.MULTIPLY_TOTAL));
                        pvp.hurt(TranscendDamage.kill(pvp.level(), player), 10f);
                    }
                }
            }
        }
        return false;
    }
}
