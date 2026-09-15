package com.mega.uom.event.eventhandler.common;

import com.mega.uom.common.attribute.ModAttributes;
import com.mega.uom.auto.AutoRegisterManager;
import com.mega.uom.common.blocks.flower.TwistedFlower1Block;
import com.mega.uom.common.blocks.flower.TwistedFlower2Block;
import com.mega.uom.common.blocks.flower.TwistedFlowerBlock;
import com.mega.uom.compat.SafeClass;
import com.mega.uom.coremod.FantasyEndingCore;
import com.mega.uom.coremod.FantasyEndingMixinPlugin;
import com.mega.uom.common.damagesource.ModDamageSources;
import com.mega.uom.common.enchantment.TwistedSoulEnchantment;
import com.mega.uom.common.enchantment.base.EnchantContext;
import com.mega.uom.common.entity.GoalAnimEntity;
import com.mega.uom.common.entity.boss.uom.UomWither;
import com.mega.uom.event.entity.*;
import com.mega.uom.common.items.FlowerTransformCeremonyItem;
import com.mega.uom.common.items.SpecialItemCheck;
import com.mega.uom.common.items.combat.sword.FantasyEndingSword;
import com.mega.uom.common.items.combat.KillsCountItem;
import com.mega.uom.common.items.combat.ShockwaveData;
import com.mega.uom.common.items.curios.TheDomainOfFadeCurio;
import com.mega.uom.common.items.curios.TwistedSoulRing;
import com.mega.uom.common.network.PacketHandler;
import com.mega.uom.common.register.ModSoundEvents;
import com.mega.uom.common.register.TargetRegister;
import com.mega.uom.mixin.ArmorStandAccessor;
import com.mega.uom.util.data.ItemEntityExpandedContext;
import com.mega.uom.util.data.LivingEntityExpandedContext;
import com.mega.uom.util.entity.EntityActuallyHurt;
import com.mega.uom.util.entity.EntityDataInjector;
import com.mega.uom.util.helper.FlashSoundPlayer;
import com.mega.uom.util.itf.ItemEntityEC;
import com.mega.uom.util.itf.LivingEntityEC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber
public class CommonHandler {
    @SubscribeEvent
    public static void itemEntityTicking(ItemEntityTickEvent event) {
        //Transform Item System
        label1: {
            if (event.phase == ItemEntityTickEvent.Phase.END) {
                ItemEntity itemEntity = (ItemEntity) event.getEntity();
                ItemStack stack = itemEntity.getItem();
                if (stack.is(TargetRegister.FLOWER_TRANSFORM) && stack.getItem() instanceof FlowerTransformCeremonyItem transformItem) {
                    ItemEntityExpandedContext itemEntityEC = ((ItemEntityEC) itemEntity).uom$itemEntityECData();
                    //Server
                    if (event.side == LogicalSide.SERVER) { 
                        if (itemEntityEC.getNeededTime() < 0)
                            itemEntityEC.setNeededTime(transformItem.transformNeededTime(stack));
                        boolean should = transformItem.shouldTransforming(stack, itemEntity, itemEntityEC);
                        if (!should) {
                            if (itemEntityEC.isTransforming())
                                //Stop
                                transformItem.stopTransforming(FlowerTransformCeremonyItem.Result.DENY, stack, itemEntity, itemEntityEC);
                            itemEntityEC.setTransforming(false);
                            itemEntityEC.setTransformTime(0);
                            break label1;
                        }
                        if (itemEntityEC.getTransformTime() < itemEntityEC.getNeededTime()) {
                            transformItem.transformingTick(LogicalSide.SERVER, stack, itemEntity, itemEntityEC);
                            itemEntityEC.setTransformTime(itemEntityEC.getTransformTime() + 1);
                            if (!itemEntityEC.isTransforming()) {
                                itemEntityEC.setTransforming(true);
                                //Start
                                transformItem.startTransforming(stack, itemEntity, itemEntityEC);
                            }
                        } else {
                            itemEntityEC.setTransforming(false);
                            //Stop
                            transformItem.stopTransforming(FlowerTransformCeremonyItem.Result.NORMAL, stack, itemEntity, itemEntityEC);
                            ItemEntity transformedItemE = new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), transformItem.transformTo(stack));
                            transformedItemE.setGlowingTag(true);
                            transformedItemE.level().addFreshEntity(transformedItemE);
                            transformedItemE.hurtMarked = true;
                            transformedItemE.setDeltaMovement(new Vec3(0, 0.03F, 0F));
                            transformedItemE.setNoGravity(true);
                            itemEntity.discard();
                        }
                    }
                    //Client
                    else {
                        if (itemEntityEC.isTransforming()) {
                            transformItem.transformingTick(LogicalSide.CLIENT, stack, itemEntity, itemEntityEC);
                        }
                    }
                }
            }
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class LazyRegister {
        public static boolean loaded = false;

        @SubscribeEvent
        public static void lazy0(EntityJoinLevelEvent event) {
            if (!loaded)
                if (event.getEntity() instanceof Player) {
                    loaded = true;
                    ShockwaveData.init();
                    FantasyEndingMixinPlugin.disableTransformLivingEntity = true;
                }
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EnchantmentEventsHandler {

        @SubscribeEvent
        public static void livingAttack(AttackEntityEvent.Pre event) {
            Player player = event.getEntity();
            EnchantContext.prePlayerAttackWeapon(event, player);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void livingDamage(LivingDamageEvent event) {
            Entity entity = event.getSource().getEntity();
            if (entity == null || entity.level().isClientSide) return;
            if (entity instanceof LivingEntity living) {
                EnchantContext.livingDamageWeapon(event, living);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void livingTick(LivingEvent.LivingTickEvent event) {
            EnchantContext.tickEffects(event.getEntity());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void livingDamageArmor(LivingDamageEvent event) {
            EnchantContext.livingDamageArmor(event, event.getSource().getEntity());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void livingHurtArmor(LivingHurtEvent event) {
            try {
                if (event.getEntity() instanceof Player player) {
                    TwistedSoulEnchantment.twistedHurt(event, event.getSource().getEntity(), TwistedSoulRing.getTwistValue(player));
                }
            } catch (Throwable throwable) {
                if (throwable instanceof NoClassDefFoundError && FMLEnvironment.dist == Dist.DEDICATED_SERVER)
                    return;
                throwable.printStackTrace();
                throwable.printStackTrace(FantasyEndingCore.stream);
                System.exit(-1);
            }
            EnchantContext.livingHurtArmor(event, event.getSource().getEntity());
        }
    }
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class LivingEntityEvents {

        @SubscribeEvent
        public static void goalAnimAiStep(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof GoalAnimEntity animEntity)
                if (!entity.isRemoved() && entity.isEffectiveAi())
                    animEntity.updateGoalAnim();
        }

        @SubscribeEvent
        public static void preMobHurtEvent(MobHurtTargetEvent.Pre event) {
            ItemStack stack;
            LivingEntity source = event.getEntity();
            if (!source.level().isClientSide)
                if ((stack = source.getMainHandItem()).is(AutoRegisterManager.ITEM_ARH().get(FantasyEndingSword.class))) {
                    Entity entity = event.getDirect();
                    if (entity instanceof PartEntity<?> partEntity)
                        entity = partEntity.getParent();
                    if (entity.isAlive()) {
                        FlashSoundPlayer.randPlaySound(source);
                        if (stack.getOrCreateTag().getInt(FantasyEndingSword.COMBO_TAG) > 5 && Math.random() > 0.5F)
                            source.playSound(ModSoundEvents.COMBO_1.get(), .15f, ((source.getRandom().nextFloat() - source.getRandom().nextFloat()) * 0.2F + 1.0F) / 0.8F);
                    }
                    FantasyEndingSword.onFeWeaponClickEntity(stack, source, entity, true, true);
                }
        }

        @SubscribeEvent
        public static void safeSpecialItems(EntityHurtEvent event) {
            if (event.getEntity() instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.isEmpty() && SpecialItemCheck.isSpItem(stack))
                    event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void enableNetherStarItemProperties(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
            if (SpecialItemCheck.isNsItem(event.getEntity().getItemInHand(InteractionHand.MAIN_HAND)) && event.getEntity().getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS))
                if (event.getTarget() instanceof LivingEntity livingEntity && !livingEntity.level().isClientSide)
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));

        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void enableFantasyEndingAttributeEffect(PreLivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide) {
                if(event.getSource().is(ModDamageSources.FE_SOURCE)){
                    return;
                } else if (event.getSource().is(ModDamageSources.DS_SOURCE)){
                    return;
                }
                Holder<DamageType> damageHolder = event.getSource().typeHolder();
                if(damageHolder instanceof Holder.Reference<DamageType> reference){
                    ResourceKey<DamageType> key = reference.key;
                    if(key != null){
                        ResourceLocation resourceLocation = key.location();
                        if(resourceLocation.getNamespace().equals("revelationfix") && resourceLocation.getPath().equals("fe_power")){
                            return;
                        }
                    }
                }
                if (player.getAttributes().hasAttribute(ModAttributes.getFeDamage())) {
                    double value = player.getAttributes().getValue(ModAttributes.getFeDamage());
                    if (value > 0F) {
                        LivingEntity target = event.getEntity();
                        EntityActuallyHurt entityActuallyHurt = new EntityActuallyHurt(target);
                        LivingEntityExpandedContext entityEC = ((LivingEntityEC) target).uom$livingECData();
                        int invulTime = target.invulnerableTime;
                        target.invulnerableTime = 0;
                        entityActuallyHurt.actuallyHurt(ModDamageSources.causeDeathFeDamage(player), (float) value);
                        entityEC.invulnerableTime = 20;
                        target.invulnerableTime = invulTime;
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void enableDreamShadowAttributeEffect(AttackEntityEvent.Pre event) {
            Player attacker = event.getEntity();
            if ( !attacker.level().isClientSide) {
                if (attacker.getAttributes().hasAttribute(ModAttributes.getDsDamage())) {
                    double value = attacker.getAttributes().getValue(ModAttributes.getDsDamage());
                    Entity target = event.getTarget();
                    if (value > 0F && target instanceof LivingEntity livingEntity) {
                        if (livingEntity instanceof ArmorStandAccessor armorStandAccessor) {
                            armorStandAccessor.callBrokenByAnything(livingEntity.damageSources().magic());
                            armorStandAccessor.callShowBreakingParticles();
                        }
                        int invul = livingEntity.invulnerableTime;
                        livingEntity.invulnerableTime = 0;
                        livingEntity.hurt(ModDamageSources.causeDeathDsDamage(attacker), (float) value);
                        livingEntity.invulnerableTime = invul;
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void domainOfFadeHurt(LivingHurtEvent event) {
            if (SafeClass.isCuriosLoaded()) {
                if (CuriosApi.getCuriosInventory(event.getEntity()).isPresent()) {
                    if (CuriosApi.getCuriosInventory(event.getEntity()).resolve().get().findFirstCurio(AutoRegisterManager.ITEM_ARH().get(TheDomainOfFadeCurio.class)).isPresent()) {
                        event.setAmount(event.getAmount() * (1 - 0.95F));
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void enableDreamShadowAttributeEffect_projectile(LivingDamageEvent event) {
            //看投票取消
            Entity entity = event.getSource().getEntity();
            if (entity instanceof LivingEntity living && !entity.level().isClientSide && event.getSource().getDirectEntity() instanceof Projectile)
                if (living.getAttributes().hasAttribute(ModAttributes.getDsDamage())) {
                    double value = living.getAttributes().getValue(ModAttributes.getDsDamage());
                    LivingEntity target = event.getEntity();
                    if (value > 0F) {
                        event.setAmount(event.getAmount()+(float) value);
                    }
                }
        }

        @SubscribeEvent
        public static void enableEvasion(LivingAttackEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.getAttributeValue(ModAttributes.EVASION.get()) > 0) {
                if (entity.getRandom().nextFloat() <= entity.getAttributeValue(ModAttributes.EVASION.get())) {
                    if (EntityDataInjector.getEvasionTime(entity) <= 0) {
                        if (!entity.level().isClientSide) {
                            PacketHandler.playSound((ServerLevel) entity.level(), entity, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, (float) entity.getRandom().triangle(0.9F, 0.3F));
                            EntityDataInjector.setEvasionTime(entity, 8);
                        }
                    }
                    event.setCanceled(true);
                }
            }
            if (EntityDataInjector.getEvasionTime(entity) > 0)
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void createTwistedFlowerEvent(LivingDeathEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;
            if (event.getSource().getEntity() instanceof UomWither uomWither) {
                ServerLevel serverLevel = (ServerLevel) entity.level();
                if (uomWither.killedEntity(serverLevel, entity)) {
                    if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(serverLevel, uomWither)) {
                        BlockPos blockpos = entity.blockPosition();
                        Class<?>[] blockClasses = new Class[]{TwistedFlower1Block.class, TwistedFlower2Block.class, TwistedFlowerBlock.class};
                        BlockState blockstate = AutoRegisterManager.INSTANCE().block.get(blockClasses[entity.getRandom().nextInt(0, 3)]).defaultBlockState();
                        if (serverLevel.isEmptyBlock(blockpos) && blockstate.canSurvive(serverLevel, blockpos)) {
                            serverLevel.setBlock(blockpos, blockstate, 3);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void killsCountHandler(LivingDeathEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                if (attacker.getMainHandItem().getItem() instanceof KillsCountItem killsCountItem) {
                    if (!EntityActuallyHurt.died(entity)) {
                        killsCountItem.addKillsCount(attacker.getMainHandItem(), 1);
                        entity.addTag("deAddedKillsCount");
                    }
                }
            } else if (entity.getKillCredit() instanceof Player attacker) {
                if (attacker.getMainHandItem().getItem() instanceof KillsCountItem killsCountItem) {
                    if (!EntityActuallyHurt.died(entity)) {
                        killsCountItem.addKillsCount(attacker.getMainHandItem(), 1);
                        entity.addTag("deAddedKillsCount");
                    }
                }
            }
        }
    }
}
