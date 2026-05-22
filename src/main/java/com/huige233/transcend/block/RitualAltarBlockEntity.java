package com.huige233.transcend.block;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.AscensionRitual;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.ritual.RitualRecipe;
import com.huige233.transcend.ritual.RitualRegistry;
import com.huige233.transcend.ritual.RitualType;
import com.huige233.transcend.world.arena.TranscendArenaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class RitualAltarBlockEntity extends BlockEntity {

    /** Round 49: Ars Nouveau Apparatus 风 — 8 pedestals (4 cardinal + 4 diagonal)。
     *  向后兼容：legacy 4-pedestal 仍工作。*/
    private static final BlockPos[] PEDESTAL_OFFSETS = {
            new BlockPos(2, 0, 0),
            new BlockPos(-2, 0, 0),
            new BlockPos(0, 0, 2),
            new BlockPos(0, 0, -2),
            // Round 49 diagonals
            new BlockPos(2, 0, 2),
            new BlockPos(2, 0, -2),
            new BlockPos(-2, 0, 2),
            new BlockPos(-2, 0, -2)
    };

    public RitualAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RITUAL_ALTAR_BE.get(), pos, state);
    }

    public void tryPerformRitual(ServerPlayer player) {
        List<RitualPedestalBlockEntity> pedestals = findPedestals();
        if (pedestals == null || pedestals.isEmpty()) {
            player.sendSystemMessage(Component.translatable("msg.transcend.ritual_structure_incomplete")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Round 49: 仅收集非空 pedestal 物品（玩家可在任意子集 pedestal 上放材料）
        List<ItemStack> items = new ArrayList<>();
        List<RitualPedestalBlockEntity> filledPedestals = new ArrayList<>();
        for (RitualPedestalBlockEntity p : pedestals) {
            if (!p.getItem().isEmpty()) {
                items.add(p.getItem());
                filledPedestals.add(p);
            }
        }

        if (items.isEmpty()) {
            player.sendSystemMessage(Component.translatable("msg.transcend.ritual_no_items")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        RitualRecipe recipe = RitualRegistry.findMatch(items);
        if (recipe == null) {
            player.sendSystemMessage(Component.translatable("msg.transcend.ritual_no_match")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Round 49: 视觉飞跃 — 物品从 pedestal 飞向 altar，再 consume
        playApparatusAnimation(player, filledPedestals);

        boolean success = executeRitual(player, recipe, filledPedestals);
        if (success) {
            for (RitualPedestalBlockEntity p : filledPedestals) {
                p.clearItem();
            }
            if (level instanceof ServerLevel sl) {
                sl.broadcastEntityEvent(player, (byte) 35);
                playSuccessBurst(sl);
            }
        }
    }

    /** Round 49: 物品飞跃动画 — 从每个有物品的 pedestal 顶飞向 altar 中心 */
    private void playApparatusAnimation(ServerPlayer player, List<RitualPedestalBlockEntity> filledPedestals) {
        if (!(level instanceof ServerLevel sl)) return;
        net.minecraft.world.phys.Vec3 altarCenter = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0, 1, 0);
        for (RitualPedestalBlockEntity p : filledPedestals) {
            net.minecraft.world.phys.Vec3 from = net.minecraft.world.phys.Vec3.atCenterOf(p.getBlockPos()).add(0, 1, 0);
            // 沿轨迹生成 12 个亮黄粒子
            for (int i = 1; i <= 12; i++) {
                double t = i / 12.0;
                double x = from.x + (altarCenter.x - from.x) * t;
                double y = from.y + (altarCenter.y - from.y) * t + Math.sin(t * Math.PI) * 0.5;
                double z = from.z + (altarCenter.z - from.z) * t;
                sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(1.0F, 0.85F, 0.4F), 1.4F),
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            // pedestal 顶圈
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        from.x + Math.cos(angle) * 0.5, from.y + 0.2, from.z + Math.sin(angle) * 0.5,
                        1, 0, 0.1, 0, 0.0);
            }
        }
    }

    /** Round 49: 仪式完成的爆发视觉 */
    private void playSuccessBurst(ServerLevel sl) {
        net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0, 1.5, 0);
        for (int i = 0; i < 60; i++) {
            double angle = sl.random.nextDouble() * Math.PI * 2;
            double pitch = sl.random.nextDouble() * Math.PI - Math.PI / 2;
            double r = 1.5 + sl.random.nextDouble() * 0.5;
            double dx = Math.cos(angle) * Math.cos(pitch) * r;
            double dy = Math.sin(pitch) * r;
            double dz = Math.sin(angle) * Math.cos(pitch) * r;
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(1.0F, 0.95F, 0.3F), 1.8F),
                    c.x, c.y, c.z, 1, dx * 0.1, dy * 0.1, dz * 0.1, 0.3);
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                c.x, c.y, c.z, 1, 0, 0, 0, 0);
        sl.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.4F);
    }

    private List<RitualPedestalBlockEntity> findPedestals() {
        if (level == null) return null;
        List<RitualPedestalBlockEntity> pedestals = new ArrayList<>();
        // Round 49: 扫描全部 8 位置，记录所有存在的 pedestal（缺位置不阻塞 — 仅"放在哪就用哪"）
        for (BlockPos offset : PEDESTAL_OFFSETS) {
            BlockPos pedestalPos = worldPosition.offset(offset);
            BlockEntity be = level.getBlockEntity(pedestalPos);
            if (be instanceof RitualPedestalBlockEntity pedestal) {
                pedestals.add(pedestal);
            }
        }
        return pedestals;
    }

    private boolean executeRitual(ServerPlayer player, RitualRecipe recipe,
                                  List<RitualPedestalBlockEntity> pedestals) {
        switch (recipe.getType()) {
            case ASCENSION -> {
                return executeAscensionRitual(player, recipe);
            }
            case CRAFT -> {
                ItemStack result = recipe.getCraftResult().copy();
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
                player.sendSystemMessage(Component.translatable("msg.transcend.ritual_craft_success")
                        .withStyle(ChatFormatting.GREEN));
                return true;
            }
            case BUFF -> {
                for (RitualRecipe.BuffEntry buff : recipe.getBuffs()) {
                    MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(buff.effect());
                    if (effect != null) {
                        player.addEffect(new MobEffectInstance(effect, buff.duration(), buff.amplifier()));
                    }
                }
                player.sendSystemMessage(Component.translatable("msg.transcend.ritual_buff_success")
                        .withStyle(ChatFormatting.GREEN));
                return true;
            }
            case SUMMON -> {
                if ("arena_invocation".equals(recipe.getId())) {
                    return TranscendArenaManager.enterArena(player);
                }
                if ("nexus_gateway".equals(recipe.getId())) {
                    return com.huige233.transcend.world.nexus.NexusManager.enterNexusDimension(player);
                }
                player.sendSystemMessage(Component.literal("Summon rituals coming soon...")
                        .withStyle(ChatFormatting.YELLOW));
                return false;
            }
            default -> { return false; }
        }
    }

    private boolean executeAscensionRitual(ServerPlayer player, RitualRecipe recipe) {
        String ritualName = recipe.getAscensionRitualName();
        AscensionRitual ritual;
        try {
            ritual = AscensionRitual.valueOf(ritualName);
        } catch (IllegalArgumentException e) {
            return false;
        }

        PlayerAscensionData data = AscensionCapability.get(player);

        if (data.isRitualCompleted(ritual)) {
            player.sendSystemMessage(Component.translatable("msg.transcend.ritual_already_done")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        if (ritual.stageIndex != data.getStage()) {
            player.sendSystemMessage(Component.translatable("msg.transcend.ritual_wrong_stage")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ritual.isMet(data)) {
            player.sendSystemMessage(Component.translatable("msg.transcend.ritual_not_ready")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        return AscensionHandler.tryCompleteRitual(player, data, ritual);
    }
}
