package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.TypedManaCrystal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Round 21: Typed Mana Conduit BE — 存储 4 aspect 独立池 + 邻居平衡 + 粒子可视化。
 *
 * <p>每 aspect 独立存储 (0-1000)。每 20 tick 扫描 6 邻居方向，对每个邻居 conduit 按差值 10% 平衡。
 * 右键空手 → 显示 4 aspect 数值；右键持 TypedManaCrystal → 消耗 1 件，对应 aspect +N (按 crystalValue)。
 */
public class ManaConduitBlockEntity extends BlockEntity {

    public static final int MAX_PER_ASPECT = 1000;
    private static final int BALANCE_INTERVAL = 20;
    private static final float BALANCE_RATE = 0.10F;

    /** 4 aspect 数组对应 ManaAspect.values() ordinal */
    private final int[] aspectMana = new int[TypedManaCrystal.ManaAspect.values().length];

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }

    public int getMana(TypedManaCrystal.ManaAspect aspect) {
        return aspectMana[aspect.ordinal()];
    }

    public void setMana(TypedManaCrystal.ManaAspect aspect, int value) {
        aspectMana[aspect.ordinal()] = Math.max(0, Math.min(MAX_PER_ASPECT, value));
        setChanged();
    }

    public int addMana(TypedManaCrystal.ManaAspect aspect, int delta) {
        int current = aspectMana[aspect.ordinal()];
        int newVal = Math.max(0, Math.min(MAX_PER_ASPECT, current + delta));
        aspectMana[aspect.ordinal()] = newVal;
        setChanged();
        return newVal - current;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaConduitBlockEntity be) {
        if (level.getGameTime() % BALANCE_INTERVAL != 0) return;
        if (!(level instanceof ServerLevel sl)) return;

        // 平衡到 6 邻居
        TypedManaCrystal.ManaAspect[] aspects = TypedManaCrystal.ManaAspect.values();
        for (Direction dir : Direction.values()) {
            BlockEntity neighborBe = level.getBlockEntity(pos.relative(dir));
            if (!(neighborBe instanceof ManaConduitBlockEntity neighbor)) continue;

            for (TypedManaCrystal.ManaAspect aspect : aspects) {
                int my = be.aspectMana[aspect.ordinal()];
                int their = neighbor.aspectMana[aspect.ordinal()];
                if (my <= their) continue; // 只主动外流，不主动吸入（避免双向竞争）
                int diff = my - their;
                int flow = (int) Math.max(1, diff * BALANCE_RATE);
                flow = Math.min(flow, diff / 2); // 不超过差值一半防震荡
                be.addMana(aspect, -flow);
                neighbor.addMana(aspect, flow);

                // 粒子流动可视化（aspect 配色，从 be 飘向 neighbor）
                float r = aspectColor(aspect, 0);
                float g = aspectColor(aspect, 1);
                float b = aspectColor(aspect, 2);
                BlockPos midPos = pos.relative(dir);
                sl.sendParticles(
                        new DustParticleOptions(new Vector3f(r, g, b), 1.2F),
                        pos.getX() + 0.5 + (midPos.getX() - pos.getX()) * 0.5,
                        pos.getY() + 0.5 + (midPos.getY() - pos.getY()) * 0.5,
                        pos.getZ() + 0.5 + (midPos.getZ() - pos.getZ()) * 0.5,
                        2, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    /** 右键交互：空手 → 显示数值；持 TypedManaCrystal → 充能 */
    public InteractionResult onUse(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.getItem() instanceof TypedManaCrystal crystal) {
            TypedManaCrystal.ManaAspect aspect = crystal.getAspect();
            int crystalValue = crystal.getCrystalValue();
            int actual = addMana(aspect, crystalValue);
            if (actual <= 0) {
                player.displayClientMessage(
                        Component.translatable("conduit.transcend.full", aspect.id)
                                .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.PASS;
            }
            if (!player.getAbilities().instabuild) heldStack.shrink(1);
            if (level != null && !level.isClientSide) {
                level.playSound(null, getBlockPos(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.6F);
            }
            player.displayClientMessage(
                    Component.translatable("conduit.transcend.injected", crystalValue,
                            Component.translatable("conduit.transcend.aspect." + aspect.id),
                            aspectMana[aspect.ordinal()], MAX_PER_ASPECT)
                            .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        // 空手 — 显示 4 aspect 数值
        StringBuilder sb = new StringBuilder();
        for (TypedManaCrystal.ManaAspect aspect : TypedManaCrystal.ManaAspect.values()) {
            sb.append("§").append(aspectChatColor(aspect))
                    .append(aspect.id).append(": ")
                    .append(aspectMana[aspect.ordinal()])
                    .append("/").append(MAX_PER_ASPECT).append("  ");
        }
        player.displayClientMessage(Component.literal(sb.toString().trim()), false);
        return InteractionResult.SUCCESS;
    }

    private static float aspectColor(TypedManaCrystal.ManaAspect aspect, int channel) {
        float[] rgb = switch (aspect) {
            case AETHER -> new float[]{0.85F, 0.95F, 0.6F};
            case BLOOD -> new float[]{0.85F, 0.10F, 0.10F};
            case COSMIC -> new float[]{0.45F, 0.50F, 0.95F};
            case TAINTED -> new float[]{0.50F, 0.10F, 0.55F};
        };
        return rgb[channel];
    }

    private static char aspectChatColor(TypedManaCrystal.ManaAspect aspect) {
        return switch (aspect) {
            case AETHER -> '6';   // gold
            case BLOOD -> 'c';    // red
            case COSMIC -> '9';   // blue
            case TAINTED -> '5';  // purple
        };
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (TypedManaCrystal.ManaAspect aspect : TypedManaCrystal.ManaAspect.values()) {
            tag.putInt("mana_" + aspect.id, aspectMana[aspect.ordinal()]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (TypedManaCrystal.ManaAspect aspect : TypedManaCrystal.ManaAspect.values()) {
            aspectMana[aspect.ordinal()] = tag.getInt("mana_" + aspect.id);
        }
    }
}
