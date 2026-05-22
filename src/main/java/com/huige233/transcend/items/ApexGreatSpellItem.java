package com.huige233.transcend.items;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * Round 19: Apex Great Spell — 终焉级仪式法术。
 *
 * <p>定位介于古卷与飞升之间：玩家手中的"一次性世界事件"。每张卷轴绑定固定 {@link ApexType}，
 * 通过长按完成 120 tick channel cast 后释放，整个法环结构会被纳入仪式核心。
 *
 * <p><b>门槛（必须同时满足）</b>：
 * <ul>
 *   <li>Ascension Stage ≥ 4（飞升者）</li>
 *   <li>玩家 16 格范围内存在 T5 (PRIMORDIAL) 活跃法环核心</li>
 *   <li>背包含 4 种相位水晶各 1 个（充能时一次性消耗）</li>
 *   <li>120 tick 不中断 channel</li>
 * </ul>
 *
 * <p>中断不退还水晶 —— 终焉法术的代价是真实的。
 */
public class ApexGreatSpellItem extends Item {

    public enum ApexType {
        /** 黑日陨落 — 30 格半径所有敌怪 200 magic dmg + 8s 火焰 */
        SOLAR_COLLAPSE("solar_collapse", 1.0F, 0.85F, 0.15F),
        /** 血盟契约 — 自损 HP → 1 换 60s 战神状态（STR4+RES2+SPD3+REG4+ABSORPTION 40） */
        BLOOD_PACT("blood_pact", 0.9F, 0.05F, 0.05F),
        /** 寰宇定锚 — 20 格半径敌怪冻结 30s（SLOW5+WEAK5+LEVITATE1） */
        COSMIC_ANCHOR("cosmic_anchor", 0.20F, 0.40F, 0.95F),
        /** 虚空解体 — 50 格半径敌怪损失 50% HP + 5s LEVITATE3 弹飞 */
        VOID_UNMAKING("void_unmaking", 0.55F, 0.10F, 0.65F);

        public final String id;
        public final float r, g, b;

        ApexType(String id, float r, float g, float b) {
            this.id = id;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final String TAG_CHARGE = "transcend_apex_charge";
    private static final int REQUIRED_STAGE = 4;

    private static int channelTicks() { return BalanceConfig.get().apex.channel_ticks; }
    private static int cooldownTicks() { return BalanceConfig.get().apex.cooldown_ticks; }
    private static int circleSearchRadius() { return BalanceConfig.get().apex.circle_search_radius; }

    private final ApexType type;

    public ApexGreatSpellItem(ApexType type) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.type = type;
    }

    public ApexType getType() {
        return type;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Gate 1: Ascension Stage 4
            PlayerAscensionData data = AscensionCapability.get(player);
            if (data.getStage() < REQUIRED_STAGE && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("apex.transcend.stage_gate", REQUIRED_STAGE)
                                .withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResultHolder.fail(stack);
            }
            // Gate 2: T5 PRIMORDIAL circle within range
            if (!hasNearbyPrimordialCircle(player, level) && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("apex.transcend.circle_gate")
                                .withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResultHolder.fail(stack);
            }
            // Gate 3: 4 typed crystals (one of each aspect)
            if (!hasAllAspects(player) && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("apex.transcend.crystal_gate")
                                .withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResultHolder.fail(stack);
            }
            // 重置 charge
            stack.getOrCreateTag().putInt(TAG_CHARGE, 0);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        CompoundTag tag = stack.getOrCreateTag();
        int progress = tag.getInt(TAG_CHARGE) + 1;
        tag.putInt(TAG_CHARGE, progress);

        // 持续视觉 + 音效
        emitChannelParticles(serverLevel, player, type, progress);
        if (progress % 20 == 0) {
            float pitch = 0.5F + (float) progress / channelTicks();
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, pitch);
        }

        // 充能完成 — 释放
        if (progress >= channelTicks()) {
            if (!player.getAbilities().instabuild) {
                // 消耗 4 个相位水晶 + 卷轴本体
                if (!consumeAllAspects(player)) {
                    // 异常路径：水晶丢失，强制中断
                    player.displayClientMessage(
                            Component.translatable("apex.transcend.crystal_lost")
                                    .withStyle(ChatFormatting.RED), true);
                    tag.remove(TAG_CHARGE);
                    player.releaseUsingItem();
                    return;
                }
                stack.shrink(1);
            }
            tag.remove(TAG_CHARGE);
            executeApex(serverLevel, player, type);
            player.releaseUsingItem();
            player.getCooldowns().addCooldown(this, cooldownTicks());
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        // 中断 — 进度清零，水晶不消耗（仅未完成 channel 时）
        if (!level.isClientSide && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_CHARGE)) {
                int progress = tag.getInt(TAG_CHARGE);
                tag.remove(TAG_CHARGE);
                if (progress > 0 && entity instanceof Player p) {
                    p.displayClientMessage(
                            Component.translatable("apex.transcend.channel_broken", progress, channelTicks())
                                    .withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }

    // ─── Gate checks ───

    private static boolean hasNearbyPrimordialCircle(Player player, Level level) {
        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRadius = 2;
        long maxDistSq = (long) circleSearchRadius() * circleSearchRadius();

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (!(e.getValue() instanceof MagicCircleCoreBlockEntity core)) continue;
                    if (!core.isActive() || !core.isStructureValid()) continue;
                    CircleTier tier = core.getDetectedTier();
                    if (tier == null || tier != CircleTier.PRIMORDIAL) continue;
                    if (e.getKey().distSqr(playerPos) <= maxDistSq) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAllAspects(Player player) {
        boolean a = false, b = false, c = false, t = false;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !(s.getItem() instanceof TypedManaCrystal tmc)) continue;
            switch (tmc.getAspect()) {
                case AETHER -> a = true;
                case BLOOD -> b = true;
                case COSMIC -> c = true;
                case TAINTED -> t = true;
            }
            if (a && b && c && t) return true;
        }
        return a && b && c && t;
    }

    /**
     * 顺序消耗 4 aspect 各一个。返回是否全部消耗成功。
     */
    private static boolean consumeAllAspects(Player player) {
        boolean[] consumed = new boolean[TypedManaCrystal.ManaAspect.values().length];
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !(s.getItem() instanceof TypedManaCrystal tmc)) continue;
            int idx = tmc.getAspect().ordinal();
            if (!consumed[idx]) {
                s.shrink(1);
                consumed[idx] = true;
            }
        }
        for (boolean ok : consumed) if (!ok) return false;
        return true;
    }

    // ─── Apex effect dispatch ───

    private static void executeApex(ServerLevel level, ServerPlayer caster, ApexType type) {
        BlockPos pos = caster.blockPosition();
        // 主释放音效 + 冲击波
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.0F, 0.4F);
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.5, 0.5, 0.5, 0.0);

        switch (type) {
            case SOLAR_COLLAPSE -> applySolarCollapse(level, caster);
            case BLOOD_PACT -> applyBloodPact(level, caster);
            case COSMIC_ANCHOR -> applyCosmicAnchor(level, caster);
            case VOID_UNMAKING -> applyVoidUnmaking(level, caster);
        }

        // 释放后向所有附近玩家广播
        caster.displayClientMessage(
                Component.translatable("apex.transcend." + type.id + ".released")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    private static void applySolarCollapse(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        AABB box = new AABB(caster.blockPosition()).inflate(a.solar_radius);
        var targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && (e instanceof Enemy || (e instanceof Mob m && !m.isAlliedTo(caster))));
        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().magic(), a.solar_damage);
            e.setSecondsOnFire(a.solar_fire_seconds);
        }
        // 视觉：大量火焰粒子从天而降
        for (int i = 0; i < 200; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * a.solar_radius;
            double x = caster.getX() + Math.cos(angle) * dist;
            double z = caster.getZ() + Math.sin(angle) * dist;
            double y = caster.getY() + 20.0;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 5, 0.5, 0.5, 0.5, 0.2);
        }
    }

    private static void applyBloodPact(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        caster.setHealth(1.0F);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, a.blood_duration_ticks, a.blood_strength_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, a.blood_duration_ticks, a.blood_resistance_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, a.blood_duration_ticks, a.blood_speed_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, a.blood_duration_ticks, a.blood_regen_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, a.blood_duration_ticks, a.blood_absorption_amp, false, true));
        // 鲜红粒子环绕
        for (int i = 0; i < 100; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double r = 1.0 + level.random.nextDouble() * 2.0;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.1F), 2.0F),
                    caster.getX() + Math.cos(angle) * r,
                    caster.getY() + level.random.nextDouble() * 2.5,
                    caster.getZ() + Math.sin(angle) * r,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void applyCosmicAnchor(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        AABB box = new AABB(caster.blockPosition()).inflate(a.cosmic_radius);
        var targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && (e instanceof Enemy || (e instanceof Mob m && !m.isAlliedTo(caster))));
        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, a.cosmic_duration_ticks, a.cosmic_slow_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, a.cosmic_duration_ticks, a.cosmic_weak_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, a.cosmic_duration_ticks, a.cosmic_levitation_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, a.cosmic_duration_ticks, a.cosmic_dig_slow_amp, false, true));
        }
        // 冰晶粒子球面
        for (int i = 0; i < 300; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = level.random.nextDouble() * Math.PI;
            double r = a.cosmic_radius;
            double x = caster.getX() + Math.sin(phi) * Math.cos(theta) * r;
            double y = caster.getY() + Math.cos(phi) * r;
            double z = caster.getZ() + Math.sin(phi) * Math.sin(theta) * r;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.2F, 0.4F, 0.95F), 2.0F),
                    x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void applyVoidUnmaking(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        AABB box = new AABB(caster.blockPosition()).inflate(a.void_radius);
        var targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && (e instanceof Enemy || (e instanceof Mob m && !m.isAlliedTo(caster))));
        for (LivingEntity e : targets) {
            float damage = e.getMaxHealth() * a.void_damage_percent;
            e.hurt(level.damageSources().magic(), damage);
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, a.void_levitation_duration, a.void_levitation_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, a.void_wither_duration, a.void_wither_amp, false, true));
        }
        // 紫色虚空粒子柱
        for (int i = 0; i < 400; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * a.void_radius;
            double y = caster.getY() + level.random.nextDouble() * 30.0;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.55F, 0.1F, 0.65F), 2.5F),
                    caster.getX() + Math.cos(angle) * dist,
                    y,
                    caster.getZ() + Math.sin(angle) * dist,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ─── Channel particles ───

    private static void emitChannelParticles(ServerLevel level, ServerPlayer player,
                                              ApexType type, int progress) {
        float t = (float) progress / channelTicks();
        // 粒子环：随充能进度收紧
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(type.r, type.g, type.b), 1.8F);
        long gt = level.getGameTime();
        int count = 12;
        double baseRadius = 3.0 - 2.5 * t; // 3.0 → 0.5
        double height = 2.5 + 1.5 * (float) Math.sin(gt * 0.15);

        for (int i = 0; i < count; i++) {
            double angle = (gt * 0.05 + i * (Math.PI * 2 / count)) % (Math.PI * 2);
            double x = player.getX() + Math.cos(angle) * baseRadius;
            double y = player.getY() + 0.5 + (height * t);
            double z = player.getZ() + Math.sin(angle) * baseRadius;
            level.sendParticles(dust, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        // 接近完成时 — 头顶蓝白冲击粒子
        if (t > 0.75F && progress % 3 == 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 2.2, player.getZ(),
                    3, 0.3, 0.1, 0.3, 0.05);
        }
    }

    // ─── Tooltip ───

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("apex.transcend." + type.id + ".desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("apex.transcend.gate.stage", REQUIRED_STAGE)
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("apex.transcend.gate.circle")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("apex.transcend.gate.crystals")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("apex.transcend.channel_time", channelTicks() / 20)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("apex.transcend.cost_warning")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
