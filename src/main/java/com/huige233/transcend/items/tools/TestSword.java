package com.huige233.transcend.items.tools;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.ModToolTiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

import java.util.UUID;

   
         
                                              
                                                  
   
/** 提供可配置伤害类型和数值的测试剑，左键清除目标无敌状态后结算一次伤害，右键打开设置界面。 */
public class TestSword extends SwordItem {
    public TestSword(){
        super(ModToolTiers.NORMAL,0,0f,(new Properties()).rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    private static final UUID MAX_HEALTH_ID = UUID.fromString("ae3f67b9-08e3-4866-8644-53770179117a");

    
    
    public static final String TAG_DAMAGE_TYPE = "TestSwordDamageTypeId";
    
    public static final String TAG_DAMAGE = "TestSwordDamage";

    
      
             
                                                                                     
                                           
                                                     
                                                            
                                                                                  

                                                
                                                                                                 
                                              
                                                                                     
                                                                         
                                                                             
                     
                                                           
                                                                                                       
                 
             
         
                     
     
      

       
                                               
                                                                    
                                        
       
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide && target instanceof LivingEntity living) {
            float dmg = getConfiguredDamage(stack);
            String typeId = getDamageTypeId(stack);

            
            living.invulnerableTime = 0;
            try {
                var f = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("lastHurt");
                f.setAccessible(true);
                f.setFloat(living, 0.0F);
            } catch (Throwable ignored) {
            }
            living.setInvulnerable(false);

            DamageSource source = TestSwordDamage.source(living.level(), player, typeId);
            
            
            living.hurt(source, dmg);
        }
        return true;                               
    }

    

    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            com.huige233.transcend.client.TestSwordScreen.open(stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    
    public static String getDamageTypeId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(TAG_DAMAGE_TYPE)
                ? "minecraft:magic" : tag.getString(TAG_DAMAGE_TYPE);
    }

    public static void setDamageTypeId(ItemStack stack, String typeId) {
        stack.getOrCreateTag().putString(TAG_DAMAGE_TYPE, typeId);
    }

    public static float getConfiguredDamage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(TAG_DAMAGE) ? 10.0F : tag.getFloat(TAG_DAMAGE);
    }

    public static void setDamage(ItemStack stack, float damage) {
        stack.getOrCreateTag().putFloat(TAG_DAMAGE, Math.max(0, damage));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
