package com.mega.uom.util.entity;

import com.mega.endinglib.mixin.accessor.AccessorLivingEntity;
import com.mega.uom.common.attribute.ModAttributes;
import com.mega.uom.auto.AutoRegisterManager;
import com.mega.uom.auto.BlockAutoRegisterHandler;
import com.mega.uom.common.blocks.flower.TwistedFlower1Block;
import com.mega.uom.common.blocks.flower.TwistedFlower2Block;
import com.mega.uom.common.blocks.flower.TwistedFlowerBlock;
import com.mega.uom.compat.SafeClass;
import com.mega.uom.compat.Wrapped;
import com.mega.uom.coremod.FantasyEndingCore;
import com.mega.uom.common.damagesource.ModDamageSources;
import com.mega.uom.common.entity.boss.uom.UomWither;
import com.mega.uom.event.entity.CatchActuallyHurt0Event;
import com.mega.uom.mixin.LivingEntityAccessor;
import com.mega.uom.mixin.SyncEntityDataAccessor;
import com.mega.uom.util.FeMapping;
import com.mega.uom.util.data.LivingEntityExpandedContext;
import com.mega.uom.util.itf.LivingEntityEC;
import com.mega.uom.util.java.ClassHandler;
import com.mega.uom.util.java.InstrumentationHelper;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 非special攻击都不playHurtSound
 */
public class EntityActuallyHurt {
    /**
     * String className
     * Integer indexOfDataItem
     */
    public static final Class<?> HEAD = LivingEntity.class;
    public static final HashMap<String, IndexAndType> entityHealthDatas = new HashMap<>();
    public static final IndexAndType NULL_DATA = new IndexAndType(-1, false);
    //Vanilla
    public static final Method getMaxHealth;
    public static Predicate<Field> isHealthField;

    static {
        try {
            getMaxHealth = LivingEntity.class.getDeclaredMethod(FeMapping.LivingEntity$METHOD$getMaxHealth.get());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        isHealthField = (field -> {
            String name = field.getName().toUpperCase().replace("_", "");
            return ( name.contains("HEALTH"))
                && (!name.contains("MAXHEALTH"))
                && (field.getType().isAssignableFrom(EntityDataAccessor.class))
                && (Modifier.isStatic(field.getModifiers()));});
    }

    public LivingEntity entity;

    public EntityActuallyHurt(LivingEntity entity) {
        this.entity = entity;
    }

    public static boolean containsIgnoreCase(String str, String searchStr) {
        return str.toUpperCase().contains(searchStr.toUpperCase());
    }

    public static void checkAndSave(LivingEntity living) {
        try {
            checkAndSave0(living);
        } catch (Throwable e) {
            e.printStackTrace();
            e.printStackTrace(FantasyEndingCore.stream);
            System.exit(-1);
        }
    }


    private static void checkAndSave0(LivingEntity living) {
        SynchedEntityData eD = living.getEntityData();
        SyncEntityDataAccessor accessor = (SyncEntityDataAccessor) eD;
        Class<? extends LivingEntity> klass = living.getClass();
        synchronized (entityHealthDatas) {
            if (!entityHealthDatas.containsKey(klass.getName()))
                if (living instanceof Player || klass.getName().startsWith("net.minecraft.")) {
                    entityHealthDatas.put(klass.getName(), new IndexAndType(LivingEntity.DATA_HEALTH_ID.getId(), true));
                }
            if (!entityHealthDatas.containsKey(klass.getName()))
                accessor.itemsById().forEach((integer, dataItem) -> {
                    synchronized (living) {
                        List<ClassHandler.FieldVarHandle> fields;
                        try {
                            fields = ClassHandler.bigFilter_allSuper(living.getClass(), HEAD, isHealthField, false, false);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        VarHandle handle = fields.size() > 1 ? fields.get(fields.size() - 1).varHandle() : fields.size() == 1 ? fields.get(0).varHandle() : null;
                        if (handle != null) {
                            //if (!hasVanillaGetHealth(klass.getDeclaredMethods()) && hasSelfGetHealth(klass.getDeclaredMethods()).size() > 0) {
                            //    EntityASMUtil.cantUseASMGetHealthAndHasSelfMethodClasses.add(klass);
                            //}
                            EntityDataAccessor<?> data = (EntityDataAccessor<?>) handle.get();
                            if (getItem((SyncEntityDataAccessor) living.entityData, data).getValue() instanceof Float) {
                                accessor.itemsById().forEach((index, dataItem0) -> {
                                    if (dataItem0.getAccessor() == data) {
                                        entityHealthDatas.put(klass.getName(), new IndexAndType(index, true));
                                    }
                                });
                                return;
                            } else if (getItem((SyncEntityDataAccessor) living.entityData, data).getValue() instanceof Double) {
                                accessor.itemsById().forEach((index, dataItem0) -> {
                                    if (dataItem0.getAccessor() == data) {
                                        entityHealthDatas.put(klass.getName(), new IndexAndType(index, false));
                                    }
                                });
                                return;
                            }

                        } else {
                            try {
                                if (dataItem.getValue() instanceof Float) {
                                    float dataHealth = getValue(living, LivingEntity.DATA_HEALTH_ID);
                                    if (Objects.equals(dataHealth, living.getHealth())) {
                                        float health = dataHealth;
                                        living.entityData.set(LivingEntity.DATA_HEALTH_ID, health-0.1F);
                                        dataHealth = getValue(living, LivingEntity.DATA_HEALTH_ID);
                                        if (Objects.equals(dataHealth, living.getHealth())) {
                                            entityHealthDatas.put(klass.getName(), new IndexAndType(LivingEntity.DATA_HEALTH_ID.getId(), true));
                                        }
                                    }
                                    if (!entityHealthDatas.containsKey(klass.getName())) {
                                        boolean flag1 = Objects.equals(living.getHealth(), (getItem(accessor, dataItem.getAccessor()).getValue()));
                                        living.setHealth(living.getHealth() + .001F);
                                        boolean flag2 = Objects.equals(living.getHealth(), dataItem.getValue());
                                        living.setHealth(living.getHealth() - .001F);
                                        if (flag1 && flag2) {
                                            entityHealthDatas.put(klass.getName(), new IndexAndType(integer, dataItem.getValue() instanceof Float));
                                        }
                                    }
                                } else if (dataItem.getValue() instanceof Double) {
                                    boolean flag1 = Objects.equals((double) living.getHealth(), dataItem.getValue());
                                    living.setHealth(living.getHealth() + .001F);
                                    boolean flag2 = Objects.equals((double) living.getHealth(), dataItem.getValue());
                                    living.setHealth(living.getHealth() - .001F);
                                    if (flag1 && flag2) {
                                        entityHealthDatas.put(klass.getName(), new IndexAndType(integer, dataItem.getValue() instanceof Float));
                                    }
                                }
                            } catch (Throwable throwable) {
                                if (!entityHealthDatas.containsKey(klass.getName())) {
                                    entityHealthDatas.put(klass.getName(), new IndexAndType(LivingEntity.DATA_HEALTH_ID.getId(), true));
                                    return;
                                }
                            }
                        }
                    }
                    if (!entityHealthDatas.containsKey(klass.getName())) {
                        accessor.itemsById().forEach((index, dataItem0) -> {
                            if (dataItem0.getAccessor() == LivingEntity.DATA_HEALTH_ID)
                                entityHealthDatas.put(klass.getName(), new IndexAndType(index, true));
                        });
                    }
                });
        }
    }

    /**
     * 存储类里最大血量数据的字段和自定义血量字段
     */
    private static void checkAndSave0_class(Class<? extends LivingEntity> clazz) throws Throwable {
        if (!entityHealthDatas.containsKey(clazz.getName()))
            if (clazz.isAssignableFrom(Player.class) || clazz.getName().startsWith("net.minecraft.")) {
                entityHealthDatas.put(clazz.getName(), new IndexAndType(LivingEntity.DATA_HEALTH_ID.getId(), true));
            }
        if (!entityHealthDatas.containsKey(clazz.getName())) {

            List<ClassHandler.FieldVarHandle> fields;
            try {
                fields = ClassHandler.bigFilter_allSuper(clazz, HEAD, isHealthField, false, false);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            VarHandle handle = fields.size() > 1 ? fields.get(fields.size() - 1).varHandle() : fields.size() == 1 ? fields.get(0).varHandle() : null;
            if (handle != null) {
                EntityDataAccessor<?> data = (EntityDataAccessor<?>) handle.get();
                boolean typeFloat = ClassHandler.getActuallyType(data.getClass()).isAssignableFrom(Float.class);
                entityHealthDatas.put(clazz.getName(), new IndexAndType(data.getId(), typeFloat));
            }
        }


    }

    public static Float getValue(LivingEntity living, EntityDataAccessor<Float> data) {
        return getItem(((SyncEntityDataAccessor) living.entityData), data).getValue();
    }

    public static <T> SynchedEntityData.DataItem<T> getItem(SyncEntityDataAccessor eD, EntityDataAccessor<T> p_135380_) {
        eD.lock().readLock().lock();

        SynchedEntityData.DataItem<T> dataitem;
        try {
            //noinspection unchecked
            dataitem = (SynchedEntityData.DataItem<T>) eD.itemsById().get(p_135380_.getId());
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting synched entity data");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Synched entity data");
            crashreportcategory.setDetail("Data ID", p_135380_);
            throw new ReportedException(crashreport);
        } finally {
            eD.lock().readLock().unlock();
        }

        return dataitem;
    }


    public static <T> void set(SyncEntityDataAccessor eD, EntityDataAccessor<T> p_276368_, T p_276363_) {
        SynchedEntityData.DataItem<T> dataitem = getItem(eD, p_276368_);
        if (ObjectUtils.notEqual(p_276363_, dataitem.getValue())) {
            setDataItemValue(dataitem, p_276363_);
            eD.caller().onSyncedDataUpdated(p_276368_);
            dataitem.setDirty(true);
            eD.setIsDirty(true);
        }

    }

    @SuppressWarnings("unchecked")
    public static void catchSetTrueHealth(LivingEntity living, float value) {
        checkAndSave(living);
        IndexAndType indexAndType = LivingEntityExpandedContext.getIndexAndType(living);
        SynchedEntityData eD = living.getEntityData();
        SyncEntityDataAccessor accessor = (SyncEntityDataAccessor) eD;
        if (indexAndType != null)
            if (indexAndType.isFloat()) {
                set(accessor, (EntityDataAccessor<Float>) accessor.itemsById().get(indexAndType.index()).getAccessor(), value);
            } else {
                set(accessor, (EntityDataAccessor<Double>) accessor.itemsById().get(indexAndType.index()).getAccessor(), (double) value);

            }
    }

    private static void dropAllDeathLoot(LivingEntity entity, DamageSource source) {
        ((AccessorLivingEntity) entity).callDropAllDeathLoot(source);
    }

    private static void createWitherRose(LivingEntity entity, LivingEntity living) {
        if (living instanceof UomWither) {
            if (!entity.level().isClientSide) {
                boolean flag = false;
                    if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(entity.level(), living)) {
                        BlockPos blockpos = entity.blockPosition();
                        BlockAutoRegisterHandler barh = AutoRegisterManager.INSTANCE().block;
                        BlockState blockstate = barh.get(TwistedFlowerBlock.class).defaultBlockState();
                        int i = entity.getRandom().nextInt(1, 4);
                        if (i == 2)
                            blockstate = barh.get(TwistedFlower1Block.class).defaultBlockState();
                        if (i == 3)
                            blockstate = barh.get(TwistedFlower2Block.class).defaultBlockState();
                        if (entity.level().isEmptyBlock(blockpos) && blockstate.canSurvive(entity.level(), blockpos)) {
                            entity.level().setBlock(blockpos, blockstate, 3);
                            flag = true;
                        }
                    }

                    if (!flag) {
                        ItemEntity itementity = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.WITHER_ROSE));
                        entity.level().addFreshEntity(itementity);
                    }

            }
        } else ((AccessorLivingEntity) entity).callCreateWitherRose(living);
    }

    private static float getSoundVolume(LivingEntity living) {
        return ((AccessorLivingEntity) living).invokeGetSoundVolume();
    }

    private static boolean checkTotemDeathProtection(LivingEntity living, DamageSource source) {
        return ((AccessorLivingEntity) living).callCheckTotemDeathProtection(source);
    }

    private static SoundEvent getDeathSound(LivingEntity living) {
        return ((AccessorLivingEntity) living).invokeGetDeathSound();
    }

    private static void playHurtSound(LivingEntity living, DamageSource source) {
        ((AccessorLivingEntity) living).invokePlayHurtSound(source);
    }

    private static void markHurt(Entity entity) {
        try {
            InstrumentationHelper.IMPL_LOOKUP().findVirtual(Entity.class, FeMapping.Entity$METHOD$markHurt.get(), MethodType.methodType(void.class))
                    .bindTo(entity).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void hurtHelmet(LivingEntity entity, DamageSource source, float amount) {
        ((AccessorLivingEntity) entity).callHurtHelmet(source, amount);
    }

    public static <T> void setDataItemValue(SynchedEntityData.DataItem<T> dataItem, T value) {
        try {
            VarHandle varHandle = InstrumentationHelper.IMPL_LOOKUP().findVarHandle(SynchedEntityData.DataItem.class, FeMapping.SynchedEntityData$DataItem$FIELD$value.get(), Object.class);
            varHandle.set(dataItem, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasVanillaGetHealth(Method[] methods) {
        return Arrays.stream(methods).anyMatch(method -> !Modifier.isStatic(method.getModifiers())
                && method.getParameterCount() == 0
                && method.getName().equals(FeMapping.LivingEntity$METHOD$getHealth.get()));
    }

    public static List<Method> hasSelfGetMaxHealth(Method[] methods) {
        return Arrays.stream(methods).filter(method -> !Modifier.isStatic(method.getModifiers())
                && method.getParameterCount() == 0
                && method.getReturnType().isAssignableFrom(float.class)
                && containsIgnoreCase(method.getName(), "HEALTH")
                && containsIgnoreCase(method.getName(), "GET")
                && containsIgnoreCase(method.getName(), "MAX")).toList();
    }

    public static List<Method> hasSelfGetHealth(Method[] methods) {
        return Arrays.stream(methods).filter(method -> !Modifier.isStatic(method.getModifiers())
                && method.getParameterCount() == 0
                && method.getReturnType().isAssignableFrom(float.class)
                && containsIgnoreCase(method.getName(), "HEALTH")
                && containsIgnoreCase(method.getName(), "GET")
                && !containsIgnoreCase(method.getName(), "MAX")).toList();
    }

    public static boolean died(LivingEntity living) {
        return living.getTags().contains("deAddedKillsCount");
    }

    public void actuallyHurt(DamageSource source, float amount) {
        actuallyHurt(source, amount, false);
    }

    public void actuallyHurt(DamageSource source, float amount, boolean special) {
        try {

            if (entity.level().isClientSide) {
                return;
            } else if (entity.isDeadOrDying() && !EntityDataInjector.getAllowHurtMethod(entity)) {
                return;
            }
            CatchActuallyHurt0Event.PrePre event = new CatchActuallyHurt0Event.PrePre(entity, source, amount);
            if (MinecraftForge.EVENT_BUS.post(event)) return;
            amount = event.getAmount();

            if (entity.isSleeping() && !entity.level().isClientSide) {
                entity.stopSleeping();
            }
            AccessorLivingEntity accessorEntity = (AccessorLivingEntity)entity;
            accessorEntity.setNoActionTime(0);
            float f = amount;
            boolean flag = false;
            if (amount > 0.0F && entity.isDamageSourceBlocked(source)) {
                net.minecraftforge.event.entity.living.ShieldBlockEvent ev = net.minecraftforge.common.ForgeHooks.onShieldBlock(entity, source, amount);
                if (!ev.isCanceled()) {
                    if (ev.shieldTakesDamage()) accessorEntity.callHurtCurrentlyUsedShield(amount);
                }
            }
            if (source.is(DamageTypeTags.IS_FREEZING) && entity.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                amount *= 5.0F;
            }

            entity.walkAnimation.setSpeed(1.5F);
            accessorEntity.setLastHurt(amount);
            entity.invulnerableTime = 20;
            LivingEntityExpandedContext entityEC = ((LivingEntityEC) entity).uom$livingECData();
            if (entityEC.invulnerableTime > 10) {
                float lastFeHurt = entityEC.lastFEHurt;
                if (amount < lastFeHurt)
                    return;
                actuallyHurt0(source, amount - lastFeHurt, special);
            } else {
                entityEC.lastFEHurt = amount;
                actuallyHurt0(source, amount, special);
            }
            entity.hurtTime = entity.hurtDuration;
            if (source.is(DamageTypeTags.DAMAGES_HELMET) && !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                accessorEntity.callHurtHelmet(source, amount);
                amount *= 0.75F;
            }

            Entity entity1 = source.getEntity();
            if (entity1 != null) {
                if (entity1 instanceof LivingEntity livingentity1) {
                    if (!source.is(DamageTypeTags.NO_ANGER)) {
                        entity.setLastHurtByMob(livingentity1);
                    }
                }

                if (entity1 instanceof Player player1) {
                    accessorEntity.setLastHurtByPlayerTime(100);
                    entity.setLastHurtByPlayer(player1);
                } else if (entity1 instanceof net.minecraft.world.entity.TamableAnimal tamableEntity) {
                    if (tamableEntity.isTame()) {
                        accessorEntity.setLastHurtByPlayerTime(100);
                        LivingEntity livingentity2 = tamableEntity.getOwner();
                        if (livingentity2 instanceof Player player) {
                            entity.setLastHurtByPlayer(player);
                        } else {
                            entity.setLastHurtByPlayer(null);
                        }
                    }
                }
            }
            if (!source.is(DamageTypeTags.NO_IMPACT)) {
                markHurt(entity);
            }

            if (entity1 != null && !source.is(DamageTypeTags.IS_EXPLOSION)) {
                double d0 = entity1.getX() - entity.getX();

                double d1;
                for (d1 = entity1.getZ() - entity.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                    d0 = (Math.random() - Math.random()) * 0.01D;
                }

                entity.knockback(0.4F, d0, d1);
                if (!flag) {
                    entity.indicateDamage(d0, d1);
                }
            }
            entity.level().broadcastDamageEvent(entity, source);
            if (entity.isDeadOrDying()) {
                if (!checkTotemDeathProtection(entity, source)) {
                    SoundEvent soundevent = getDeathSound(entity);
                    if (soundevent != null) {
                        if (special) {
                            entity.playSound(soundevent, getSoundVolume(entity), entity.getVoicePitch());
                        }
                    }

                    die(entity, source);
                }
            } else {
                if (special)
                    playHurtSound(entity, source);
            }
            accessorEntity.setLastDamageSource(source);
            accessorEntity.setLastDamageStamp(entity.level().getGameTime());
            if (entity instanceof ServerPlayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) entity, source, f, amount, flag);
            }
            if (entity1 instanceof ServerPlayer) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity1, entity, source, f, amount, flag);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void actuallyHurtForDelta(DamageSource source, float amount, boolean special) {
        try {
            if (entity.level().isClientSide) {
                return;
            } else if (entity.isDeadOrDying() && !EntityDataInjector.getAllowHurtMethod(entity)) {
                return;
            }
            CatchActuallyHurt0Event.PrePre event = new CatchActuallyHurt0Event.PrePre(entity, source, amount);
            if (MinecraftForge.EVENT_BUS.post(event)) return;
            amount = event.getAmount();

            if (entity.isSleeping() && !entity.level().isClientSide) {
                entity.stopSleeping();
            }
            AccessorLivingEntity accessorEntity = (AccessorLivingEntity)entity;
            accessorEntity.setNoActionTime(0);
            float f = amount;
            boolean flag = false;
            if (amount > 0.0F && entity.isDamageSourceBlocked(source)) {
                net.minecraftforge.event.entity.living.ShieldBlockEvent ev = net.minecraftforge.common.ForgeHooks.onShieldBlock(entity, source, amount);
                if (!ev.isCanceled()) {
                    if (ev.shieldTakesDamage()) accessorEntity.callHurtCurrentlyUsedShield(amount);
                }
            }
            if (source.is(DamageTypeTags.IS_FREEZING) && entity.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                amount *= 5.0F;
            }

            entity.walkAnimation.setSpeed(1.5F);
            accessorEntity.setLastHurt(amount);
            entity.invulnerableTime = 20;
            LivingEntityExpandedContext entityEC = ((LivingEntityEC) entity).uom$livingECData();
            if (entityEC.invulnerableTime > 10) {
                float lastFeHurt = entityEC.lastFEHurt;
                if (amount < lastFeHurt)
                    return;
                actuallyHurt0ForDelta(source, amount - lastFeHurt, special);
            } else {
                entityEC.lastFEHurt = amount;
                actuallyHurt0ForDelta(source, amount, special);
            }
            entity.hurtTime = entity.hurtDuration;
            if (source.is(DamageTypeTags.DAMAGES_HELMET) && !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                accessorEntity.callHurtHelmet(source, amount);
                amount *= 0.75F;
            }

            Entity entity1 = source.getEntity();
            if (entity1 != null) {
                if (entity1 instanceof LivingEntity livingentity1) {
                    if (!source.is(DamageTypeTags.NO_ANGER)) {
                        entity.setLastHurtByMob(livingentity1);
                    }
                }

                if (entity1 instanceof Player player1) {
                    accessorEntity.setLastHurtByPlayerTime(100);
                    entity.setLastHurtByPlayer(player1);
                } else if (entity1 instanceof net.minecraft.world.entity.TamableAnimal tamableEntity) {
                    if (tamableEntity.isTame()) {
                        accessorEntity.setLastHurtByPlayerTime(100);
                        LivingEntity livingentity2 = tamableEntity.getOwner();
                        if (livingentity2 instanceof Player player) {
                            entity.setLastHurtByPlayer(player);
                        } else {
                            entity.setLastHurtByPlayer(null);
                        }
                    }
                }
            }
            if (!source.is(DamageTypeTags.NO_IMPACT)) {
                markHurt(entity);
            }

            if (entity1 != null && !source.is(DamageTypeTags.IS_EXPLOSION)) {
                double d0 = entity1.getX() - entity.getX();

                double d1;
                for (d1 = entity1.getZ() - entity.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                    d0 = (Math.random() - Math.random()) * 0.01D;
                }

                entity.knockback(0.4F, d0, d1);
                if (!flag) {
                    entity.indicateDamage(d0, d1);
                }
            }
            entity.level().broadcastDamageEvent(entity, source);
            if (entity.isDeadOrDying()) {
                if (!checkTotemDeathProtection(entity, source)) {
                    SoundEvent soundevent = getDeathSound(entity);
                    if (soundevent != null) {
                        if (special) {
                            entity.playSound(soundevent, getSoundVolume(entity), entity.getVoicePitch());
                        }
                    }

                    die(entity, source);
                }
            } else {
                if (special)
                    playHurtSound(entity, source);
            }
            accessorEntity.setLastDamageSource(source);
            accessorEntity.setLastDamageStamp(entity.level().getGameTime());
            if (entity instanceof ServerPlayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) entity, source, f, amount, flag);
            }
            if (entity1 instanceof ServerPlayer) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity1, entity, source, f, amount, flag);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    //only server
    public void die(LivingEntity livingEntity, DamageSource p_21014_) {
        if (livingEntity.level().isClientSide) return;
        try {
            livingEntity.die(p_21014_);
        } catch (ClassCastException exception) {
            exception.printStackTrace();
        }
        LivingEntityAccessor accessor = (LivingEntityAccessor) livingEntity;
        if (!livingEntity.isRemoved() && !accessor.dead()) {
            Entity entity = p_21014_.getEntity();
            LivingEntity livingentity = livingEntity.getKillCredit();
            if (accessor.deathScore() >= 0 && livingentity != null) {
                livingentity.awardKillScore(livingEntity, accessor.deathScore(), p_21014_);
            }

            if (livingEntity.isSleeping()) {
                livingEntity.stopSleeping();
            }

            accessor.setDead(true);
            livingEntity.getCombatTracker().recheckStatus();
            Level level = livingEntity.level();
            if (level instanceof ServerLevel serverlevel) {
                if (entity == null || entity.killedEntity(serverlevel, livingEntity)) {
                    livingEntity.gameEvent(GameEvent.ENTITY_DIE);
                    dropAllDeathLoot(livingEntity, p_21014_);
                    createWitherRose(livingEntity, livingentity);
                }

                livingEntity.level().broadcastEntityEvent(livingEntity, (byte) 3);
            }

            livingEntity.setPose(Pose.DYING);

            if (livingEntity.getHealth() > 0.0F) {
                catchSetTrueHealth(livingEntity, 0.0F);
            }
        }
    }

    public void actuallyHurt0(DamageSource source, float amount, boolean special) {
        if (amount <= 0) return;
        if (!special && source.is(ModDamageSources.FE_SOURCE))
            amount *= (2d - entity.getAttributeValue(ModAttributes.getFeDamageResistance()));
        CatchActuallyHurt0Event.Pre event = new CatchActuallyHurt0Event.Pre(entity, source, amount);
        if (MinecraftForge.EVENT_BUS.post(event)) return;
        amount = event.getAmount();
        entity.getCombatTracker().recordDamage(source, amount);
        if (amount == Float.POSITIVE_INFINITY) {
            entity.setHealth(Float.NEGATIVE_INFINITY);
            if (special) catchSetTrueHealth(entity, Float.NEGATIVE_INFINITY);
        } else {
            float finalHealth = Math.min(entity.getHealth() - amount, entity.getMaxHealth());
            entity.setHealth(finalHealth);
            if (special) catchSetTrueHealth(entity, finalHealth);
        }
        entity.gameEvent(GameEvent.ENTITY_DAMAGE);
        if (!entity.isDeadOrDying()) entity.deathTime = 0;
    }

    public void actuallyHurt0ForDelta(DamageSource source, float amount, boolean special) {
        if (amount <= 0) return;
        if (!special && source.is(ModDamageSources.FE_SOURCE))
            amount *= (2d - entity.getAttributeValue(ModAttributes.getFeDamageResistance()));
        CatchActuallyHurt0Event.Pre event = new CatchActuallyHurt0Event.Pre(entity, source, amount);
        if (MinecraftForge.EVENT_BUS.post(event)) return;
        amount = event.getAmount();
        entity.getCombatTracker().recordDamage(source, amount);
        if (amount == Float.POSITIVE_INFINITY) {
            EntityASMUtil.setHealthDelta(entity, Float.NEGATIVE_INFINITY);
        } else {
            float currentHealth = Math.min(entity.getHealth() - amount, entity.getMaxHealth());
            EntityASMUtil.addDelta(entity, -(entity.getHealth() - currentHealth));
        }
        entity.gameEvent(GameEvent.ENTITY_DAMAGE);
        if (!entity.isDeadOrDying()) entity.deathTime = 0;
    }

    public void playDeathSound() {
        SoundEvent soundEvent = getDeathSound(entity);
        if (soundEvent != null)
            entity.playSound(soundEvent, getSoundVolume(entity), entity.getVoicePitch());
    }

    public record IndexAndType(int index, boolean isFloat) {
    }
    public static boolean isApostleSecondPhase(Entity entity) {
        if (!SafeClass.isGoetyLoaded()) return false;
        return Wrapped.isApostleSecondPhase(entity);
    }
    public static boolean isSecondPhaseApostleOrNot(Entity entity) {
        if (!SafeClass.isGoetyLoaded()) return false;
        return Wrapped.isSecondPhaseApostleOrNot(entity);
    }
    public static boolean isApollyon(Entity entity) {
        if (!SafeClass.isGoetyRevLoaded()) return false;
        return Wrapped.isApollyon(entity);
    }
    public static boolean isApollyonSecondPhase(Entity entity) {
        if (!isApollyon(entity)) return false;
        return Wrapped.isApollyonSecondPhase(entity);
    }
}
