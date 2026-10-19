package com.huige233.transcend.handle.tech;

import com.huige233.transcend.entity.TestDummy;
import com.huige233.transcend.items.tech.PhaseShield;
import com.huige233.transcend.tech.combat.TechHitSnapshot;
import com.huige233.transcend.tech.rank.TechRank;
import com.huige233.transcend.entity.projectile.ParticleBolt;
import com.huige233.transcend.tech.shield.ShieldDamageContext;
import com.huige233.transcend.tech.shield.ShieldResolution;
import com.huige233.transcend.util.CuriosFinder;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CPhaseShieldState;
import com.huige233.transcend.tech.shield.PhaseShieldStatus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;


/** 统一结算手持与饰品相位护盾的伤害吸收和电容消耗，处理负面效果拦截并同步显示状态。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PhaseShieldHandler {
    private static final Map<ServerPlayer, PhaseShieldStatus> LAST_SENT = new WeakHashMap<>();
    private static final Map<ServerPlayer, String> LAST_SENT_KEY = new WeakHashMap<>();

    /**
     * 处理科技枪的直接生命层攻击。直接改血不是原版伤害事件，必须在生命账本写入前
     * 单独经过护盾；否则普通 LivingHurtEvent 拦截不到这一层。返回未被护盾转化的
     * 生命伤害，0 表示本次直接生命攻击已被护盾完全承受。
     */
    public static float absorbDirectHealthAttack(LivingEntity target, float amount, int penetrationStrength,
                                                 TechRank attackingRank) {
        if (!(target instanceof Player player) || player.level().isClientSide
                || amount <= 0.0F || !Float.isFinite(amount) || isLegacyProtected(player)) return amount;
        List<ItemStack> shields = activeShields(player);
        float remaining = amount;
        java.util.Set<ItemStack> settled = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (ItemStack stack : shields) {
            if (remaining <= 0.0001F) break;
            if (!settled.add(stack) || PhaseShield.getEnergy(stack) < PhaseShield.MIN_USE_ENERGY) continue;
            ShieldResolution result = PhaseShield.absorb(stack, new ShieldDamageContext(
                    TestDummy.DamageCategory.PROJECTILE, remaining, false, penetrationStrength, attackingRank));
            if (result.absorbedAny()) {
                remaining = result.remainingDamage();
            }
        }
        if (remaining < amount) syncState(player);
        return Math.max(0.0F, remaining);
    }

    public static PhaseShieldStatus displayStatus(Player player) {
        if (!player.isAlive() || player.isSpectator()) return PhaseShieldStatus.ABSENT;
        ItemStack held = findPhaseShield(player);
        List<SlotResult> curios = CuriosFinder.findCurios(player, stack -> stack.getItem() instanceof PhaseShield);
        if (!held.isEmpty() || !curios.isEmpty()) {
            boolean powered = (!held.isEmpty() && PhaseShield.getEnergy(held) >= PhaseShield.MIN_USE_ENERGY)
                    || curios.stream().anyMatch(slot ->
                    PhaseShield.getEnergy(slot.stack()) >= PhaseShield.MIN_USE_ENERGY);
            return PhaseShieldStatus.evaluate(true, !isLegacyProtected(player), powered);
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof PhaseShield) return PhaseShieldStatus.INACTIVE;
        }
        return PhaseShieldStatus.ABSENT;
    }

    public static void syncState(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        PhaseShieldStatus status = displayStatus(player);
        float charge = currentCharge(player);
        String key = status.name() + ':' + Math.round(charge * 100.0F);
        PhaseShieldStatus previousStatus = LAST_SENT.put(serverPlayer, status);
        String previousKey = LAST_SENT_KEY.put(serverPlayer, key);
        if (previousStatus == status && key.equals(previousKey)) return;
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                new S2CPhaseShieldState(player.getUUID(), status, charge));
    }

    private static void forceSync(Player player) {
        LAST_SENT.remove(player);
        syncState(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { forceSync(event.getEntity()); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) { forceSync(event.getEntity()); }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) { forceSync(event.getEntity()); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity());
        LAST_SENT_KEY.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof Player target) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer),
                    new S2CPhaseShieldState(target.getUUID(), displayStatus(target), currentCharge(target)));
        }
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof Player target) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer),
                    new S2CPhaseShieldState(target.getUUID(), PhaseShieldStatus.ABSENT, 0.0F));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !event.player.level().isClientSide) {
            for (ItemStack shield : activeShields(event.player)) {
                if (PhaseShield.supplyOxygen(shield, event.player)) break;
            }
            syncState(event.player);
        }
    }

    @SubscribeEvent
    public static void onNegativeEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getEffectInstance() == null
                || event.getEffectInstance().getEffect().isBeneficial()) return;
        float cost = Math.max(1L, (long) event.getEffectInstance().getAmplifier() + 1L) * 5.0F;
        for (ItemStack shield : activeShields(player)) {
            PhaseShield.initialize(shield);
            if (!PhaseShield.covers(shield, TestDummy.DamageCategory.MAGIC.index)
                    || PhaseShield.capacitorEnergy(shield) < cost) continue;
            PhaseShield.drainCapacitor(shield, cost);
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            syncState(player);
            break;
        }
    }

    private static List<ItemStack> activeShields(Player player) {
        if (!player.isAlive() || player.isSpectator() || isLegacyProtected(player)) return List.of();
        java.util.ArrayList<ItemStack> shields = new java.util.ArrayList<>();
        ItemStack held = findPhaseShield(player);
        if (!held.isEmpty()) shields.add(held);
        for (SlotResult slot : CuriosFinder.findCurios(player,
                stack -> stack.getItem() instanceof PhaseShield)) shields.add(slot.stack());
        return shields;
    }
    
    @SubscribeEvent
    public static void onPhaseShieldHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.isCanceled() || isLegacyProtected(player)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float original = event.getAmount();
        if (!Float.isFinite(original) || original <= 0.0F) return;
        List<ItemStack> shields = activeShields(player);
        if (shields.isEmpty()) return;
        TechHitSnapshot snapshot = TechHitSnapshot.capture(player, source);
        TestDummy.DamageCategory category = TestDummy.DamageCategory.byIndex(
                snapshot.classification().categoryIndex());
        java.util.Set<ItemStack> settled = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        float remaining = original;
        float spent = 0.0F;
        int participating = 0;
        for (ItemStack stack : shields) {
            if (remaining <= 0.0F) break;
            if (!settled.add(stack) || PhaseShield.getEnergy(stack) < PhaseShield.MIN_USE_ENERGY) continue;
            float multiplier = PhaseShield.energyMultiplier(stack);
            if (!Float.isFinite(multiplier) || multiplier <= 0.0F) continue;
            // 相位场按自身类别覆盖防护，不沿用原版手持盾的 BYPASSES_SHIELD 标签。
            ShieldResolution result = PhaseShield.absorb(stack, new ShieldDamageContext(
                    category, remaining, false, snapshot.shieldStrength(),
                    source.getDirectEntity() instanceof ParticleBolt bolt ? bolt.techRank() : null));
            if (!result.absorbedAny()) continue;
            remaining = result.remainingDamage();
            spent += result.shieldEnergySpent();
            participating++;
        }
        if (participating == 0) return;
        event.setAmount(remaining);
        playAbsorb(player, new ShieldResolution(original, original - remaining, remaining, spent, participating, 0));
        syncState(player);
    }

    private static float currentCharge(Player player) {
        return (float) activeShields(player).stream().mapToDouble(PhaseShieldHandler::chargeRatio).max().orElse(0);
    }

    private static float chargeRatio(ItemStack stack) {
        return PhaseShield.getEnergy(stack) / Math.max(1.0F, PhaseShield.getMaxEnergy(stack));
    }

    private static boolean isLegacyProtected(Player player) {
        return com.huige233.transcend.items.TranscendShield.hasTranscendShield(player)
                || com.huige233.transcend.util.ArmorUtils.fullEquipped(player)
                || CuriosFinder.hasCurio(player, com.huige233.transcend.init.ModItems.transcend_curio.get());
    }
    private static ItemStack findPhaseShield(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof PhaseShield && player.isUsingItem()
                    && player.getUsedItemHand() == hand) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void playBroken(Player player) {
        if (player.level() instanceof ServerLevel level) {
            ServerVisualBroadcaster.shockwave(level, player.position(), 0.8F, 1.0F, 0.3F, 0.3F, 8);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    private static void playAbsorb(Player player, ShieldResolution result) {
        if (player.level() instanceof ServerLevel level) {
            float intensity = result.fullyAbsorbed() ? 1.0F : 1.25F;
            ServerVisualBroadcaster.shieldRipple(level, player.position(), 0.9F * intensity,
                    0.25F, 0.85F, 1.0F, 10);
            ServerVisualBroadcaster.shockwave(level, player.position(), 0.35F * intensity,
                    0.35F, 0.9F, 1.0F, 6);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.65F, result.fullyAbsorbed() ? 1.25F : 1.05F);
        }
    }
}
