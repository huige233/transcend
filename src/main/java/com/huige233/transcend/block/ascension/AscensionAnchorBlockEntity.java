package com.huige233.transcend.block.ascension;

import com.huige233.transcend.ascension.AscensionRitual;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.block.mana.ManaTransmitCrystalBlockEntity;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 飞升锚点方块实体。 */
public class AscensionAnchorBlockEntity extends BlockEntity {

    public static final double DEFAULT_ANGLE_TOLERANCE_RAD = Math.toRadians(5.0);

    public static final double DEFAULT_RADIAL_TOLERANCE = 0.5;

    public static final int DEFAULT_Y_TOLERANCE = 1;

    public static final int RITUAL_DURATION_TICKS = 100;

    private boolean ritualActive = false;
    private int ritualTimer = 0;
    @Nullable private AscensionRitual ritualTarget = null;
    @Nullable private UUID ritualPlayer = null;
    private final List<BlockPos> ritualCrystals = new ArrayList<>();

    @Nullable private UUID soulMarkOwner = null;

    public AscensionAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASCENSION_ANCHOR_BE.get(), pos, state);
    }

    public boolean isRitualActive() { return ritualActive; }
    @Nullable public AscensionRitual getRitualTarget() { return ritualTarget; }
    public int getRitualTimer() { return ritualTimer; }
    public List<BlockPos> getRitualCrystals() { return ritualCrystals; }

    public float getRitualProgress() {
        return ritualActive ? Math.min(1.0F, ritualTimer / (float) RITUAL_DURATION_TICKS) : 0.0F;
    }

    @Nullable public UUID getSoulMarkOwner() { return soulMarkOwner; }

    public boolean isSoulMarked() { return soulMarkOwner != null; }

    public void setSoulMarkOwner(@Nullable UUID uuid) {
        this.soulMarkOwner = uuid;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void tryStartRitual(ServerLevel level, Player player, PlayerAscensionData data, AscensionRitual ritual) {

        PatternSpec spec = patternFor(ritual);

        List<BlockPos> matched = detectPattern(level, getBlockPos(),
                spec.crystalCount, spec.radius,
                spec.angleToleranceRad, spec.radialTolerance, spec.yTolerance);
        if (matched == null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.pattern_invalid",
                            spec.crystalCount, spec.radius)
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!ritual.isMet(data)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.prereq_unmet",
                            ritual.getDisplayName(),
                            ritual.requiredKills, data.getTotalKills(),
                            ritual.requiredCasts, data.getTotalCasts(),
                            ritual.requiredLevel, data.getInsightLevel())
                            .withStyle(ChatFormatting.GOLD), true);
            return;
        }

        if (!ritual.hasItems(player)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.items_unmet",
                            ritual.requiredItemCount,
                            ritual.requiredItem.get().getDescription())
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        int baseCost = spec.manaBaseCost;
        int perCost = spec.manaPerCrystal;
        if (!drainNetworkMana(level, matched, baseCost, perCost, true)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.mana_unmet",
                            baseCost + perCost * spec.crystalCount)
                            .withStyle(ChatFormatting.AQUA), true);
            return;
        }

        drainNetworkMana(level, matched, baseCost, perCost, false);

        ritualActive = true;
        ritualTimer = 0;
        ritualTarget = ritual;
        ritualPlayer = player.getUUID();
        ritualCrystals.clear();
        ritualCrystals.addAll(matched);
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);

        level.playSound(null, getBlockPos(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.2F, 0.8F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.ascension_anchor.ritual_started",
                        ritual.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AscensionAnchorBlockEntity be) {
        if (!be.ritualActive) return;
        if (!(level instanceof ServerLevel sl)) return;

        be.ritualTimer++;

        if (be.ritualTimer % 20 == 0) {
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 1.0F + be.getRitualProgress() * 0.5F);
        }

        if (be.ritualTimer % 4 == 0) {
            for (BlockPos crystal : be.ritualCrystals) {

                double t = level.random.nextDouble();
                double px = (crystal.getX() + 0.5) * t + (pos.getX() + 0.5) * (1 - t);
                double py = (crystal.getY() + 0.5) * t + (pos.getY() + 1.2) * (1 - t);
                double pz = (crystal.getZ() + 0.5) * t + (pos.getZ() + 0.5) * (1 - t);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        px, py, pz, 1, 0.05, 0.05, 0.05, 0.02);
            }

            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2 + be.getRitualProgress() * 2.0,
                    pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
        }

        if (be.ritualTimer >= RITUAL_DURATION_TICKS) {
            be.completeRitual(sl);
        } else {

            if (be.ritualTimer % 5 == 0) {
                sl.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AscensionAnchorBlockEntity be) {
        if (!be.ritualActive) return;
        long gameTime = level.getGameTime();
        if (gameTime % 6 != 0) return;

        double progress = be.getRitualProgress();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.0 + progress * 0.5;
        double cz = pos.getZ() + 0.5;

        double angle = (gameTime * 0.15) % (2 * Math.PI);
        double r = 0.5 + 0.2 * Math.sin(gameTime * 0.1);
        for (int i = 0; i < 3; i++) {
            double a = angle + i * (2 * Math.PI / 3);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    cx + Math.cos(a) * r, cy + Math.random() * 0.5, cz + Math.sin(a) * r,
                    0, -0.05, 0);
        }
    }

    private void completeRitual(ServerLevel level) {
        boolean success = false;
        if (ritualPlayer != null && ritualTarget != null) {
            Player p = level.getPlayerByUUID(ritualPlayer);
            if (p != null) {

                if (ritualTarget.consumeItems(p)) {

                    PlayerAscensionData data = com.huige233.transcend.ascension.AscensionCapability.get(p);
                    if (data.tryCompleteRitual(ritualTarget)) {
                        success = true;

                        if (p instanceof ServerPlayer sp) {
                            com.huige233.transcend.ascension.AscensionHandler.syncToClient(sp, data);
                        }

                        if (p instanceof ServerPlayer sp) {
                            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                                    Component.translatable("title.transcend.ascended",
                                            ritualTarget.getDisplayName())
                                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
                            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                                    ritualTarget.getRewardText()));
                        }
                        p.displayClientMessage(ritualTarget.getRewardText(), false);
                    }
                }
            }
        }

        level.playSound(null, getBlockPos(),
                success ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS, 1.5F, success ? 1.5F : 0.5F);

        ritualActive = false;
        ritualTimer = 0;
        ritualTarget = null;
        ritualPlayer = null;
        ritualCrystals.clear();
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Nullable
    public static List<BlockPos> detectPattern(Level level, BlockPos anchor, int N, double radius,
                                                double angleToleranceRad, double radialTolerance,
                                                int yTolerance) {

        int range = (int) Math.ceil(radius) + 1;
        List<BlockPos> ring = new ArrayList<>();

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -yTolerance; dy <= yTolerance; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    m.set(anchor.getX() + dx, anchor.getY() + dy, anchor.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(m);
                    if (!(be instanceof ManaTransmitCrystalBlockEntity)) continue;

                    double cdx = (m.getX() + 0.5) - (anchor.getX() + 0.5);
                    double cdz = (m.getZ() + 0.5) - (anchor.getZ() + 0.5);
                    double d = Math.sqrt(cdx * cdx + cdz * cdz);
                    if (Math.abs(d - radius) <= radialTolerance) {
                        ring.add(new BlockPos(m));
                    }
                }
            }
        }

        if (ring.size() != N) return null;

        ring.sort(Comparator.comparingDouble(p -> Math.atan2(
                (p.getZ() + 0.5) - (anchor.getZ() + 0.5),
                (p.getX() + 0.5) - (anchor.getX() + 0.5))));

        double expectedStep = 2.0 * Math.PI / N;
        for (int i = 0; i < N; i++) {
            BlockPos a = ring.get(i);
            BlockPos b = ring.get((i + 1) % N);
            double angA = Math.atan2(
                    (a.getZ() + 0.5) - (anchor.getZ() + 0.5),
                    (a.getX() + 0.5) - (anchor.getX() + 0.5));
            double angB = Math.atan2(
                    (b.getZ() + 0.5) - (anchor.getZ() + 0.5),
                    (b.getX() + 0.5) - (anchor.getX() + 0.5));
            double diff = angB - angA;
            if (diff < 0) diff += 2.0 * Math.PI;
            if (Math.abs(diff - expectedStep) > angleToleranceRad) return null;
        }

        return ring;
    }

    private static boolean drainNetworkMana(Level level, List<BlockPos> crystals,
                                             int baseCost, int perCrystalCost, boolean simulate) {
        if (crystals.isEmpty()) return baseCost == 0;
        int n = crystals.size();
        int perShare = baseCost / n + perCrystalCost;

        for (BlockPos crystalPos : crystals) {
            IManaHandler source = resolveCrystalSource(level, crystalPos);
            if (source == null || !source.canExtract()) return false;
            int extracted = source.extractMana(perShare, simulate);
            if (extracted < perShare) return false;
        }
        return true;
    }

    @Nullable
    private static IManaHandler resolveCrystalSource(Level level, BlockPos crystalPos) {
        final int SCAN_RADIUS = 4;
        BlockPos below = crystalPos.below();
        BlockEntity belowBe = level.getBlockEntity(below);
        if (belowBe != null) {
            IManaHandler cap = belowBe.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
            if (cap != null) return cap;
        }
        IManaHandler best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    m.set(below.getX() + dx, below.getY() + dy, below.getZ() + dz);
                    BlockEntity nb = level.getBlockEntity(m);
                    if (nb == null) continue;
                    IManaHandler cap = nb.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
                    if (cap == null) continue;
                    double d = below.distSqr(m);
                    if (d < bestDistSq) {
                        bestDistSq = d;
                        best = cap;
                    }
                }
            }
        }
        return best;
    }

    private static PatternSpec defaultPatternFor(AscensionRitual ritual) {
        return switch (ritual) {
            case AWAKENING     -> new PatternSpec(3, 3.0, 200, 100);
            case TEMPERING     -> new PatternSpec(4, 4.0, 400, 200);
            case PURIFICATION  -> new PatternSpec(5, 5.0, 800, 400);
            case TRANSCENDENCE -> new PatternSpec(6, 6.0, 2000, 1000);
        };
    }

    public static PatternSpec patternFor(AscensionRitual ritual) {
        AscensionPatternConfig cfg = AscensionPatternRegistry.getInstance().get(ritual);
        if (cfg != null) {
            return new PatternSpec(
                    cfg.crystalCount(), cfg.radius(),
                    cfg.manaBaseCost(), cfg.manaPerCrystal(),
                    cfg.angleToleranceRad(), cfg.radialTolerance(), cfg.yTolerance());
        }
        return defaultPatternFor(ritual);
    }

    public record PatternSpec(
            int crystalCount,
            double radius,
            int manaBaseCost,
            int manaPerCrystal,
            double angleToleranceRad,
            double radialTolerance,
            int yTolerance
    ) {
        public PatternSpec(int crystalCount, double radius, int manaBaseCost, int manaPerCrystal) {
            this(crystalCount, radius, manaBaseCost, manaPerCrystal,
                    DEFAULT_ANGLE_TOLERANCE_RAD, DEFAULT_RADIAL_TOLERANCE, DEFAULT_Y_TOLERANCE);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("RitualActive", ritualActive);
        tag.putInt("RitualTimer", ritualTimer);
        if (ritualTarget != null) tag.putString("RitualTarget", ritualTarget.name());
        if (ritualPlayer != null) tag.putUUID("RitualPlayer", ritualPlayer);
        if (!ritualCrystals.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (BlockPos p : ritualCrystals) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("x", p.getX());
                entry.putInt("y", p.getY());
                entry.putInt("z", p.getZ());
                list.add(entry);
            }
            tag.put("RitualCrystals", list);
        }
        if (soulMarkOwner != null) tag.putUUID("SoulMarkOwner", soulMarkOwner);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ritualActive = tag.getBoolean("RitualActive");
        ritualTimer = tag.getInt("RitualTimer");
        ritualTarget = tag.contains("RitualTarget") ? safeRitual(tag.getString("RitualTarget")) : null;
        ritualPlayer = tag.hasUUID("RitualPlayer") ? tag.getUUID("RitualPlayer") : null;
        ritualCrystals.clear();
        if (tag.contains("RitualCrystals", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag list = tag.getList("RitualCrystals", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ritualCrystals.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
            }
        }
        soulMarkOwner = tag.hasUUID("SoulMarkOwner") ? tag.getUUID("SoulMarkOwner") : null;
    }

    @Nullable
    private static AscensionRitual safeRitual(String name) {
        try {
            return AscensionRitual.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
