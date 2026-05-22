package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.TypedManaCrystal;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Round 22: Functional Mana Block 通用 BE — 按 FunctionType 派发逻辑。
 *
 * <p>所有 sink (FURNACE/SENTINEL/HARVESTER) 都要：
 * <ol>
 *   <li>每 cooldownTicks tick 检查一次</li>
 *   <li>扫 6 邻居 ManaConduitBlockEntity，找有 manaPerOp 储备的</li>
 *   <li>找到 → 扣除 manaPerOp → 执行 operation</li>
 *   <li>没找到 → 闲置</li>
 * </ol>
 *
 * <p>GENERATOR 反向：从 chunk 环境抽取魔力浓度 → 给邻居 conduit 注入 Tainted。
 */
public class FunctionalManaBlockEntity extends BlockEntity {

    private final FunctionalManaBlock.FunctionType type;
    private int tickCount = 0;

    public FunctionalManaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUNCTIONAL_MANA_BE.get(), pos, state);
        // 默认 FURNACE，会通过 block 参数化 ctor 覆盖
        this.type = (state.getBlock() instanceof FunctionalManaBlock fmb)
                ? fmb.getFunctionType()
                : FunctionalManaBlock.FunctionType.FURNACE;
    }

    public FunctionalManaBlockEntity(BlockPos pos, BlockState state, FunctionalManaBlock.FunctionType type) {
        super(ModBlockEntities.FUNCTIONAL_MANA_BE.get(), pos, state);
        this.type = type;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FunctionalManaBlockEntity be) {
        be.tickCount++;
        if (be.tickCount < be.type.cooldownTicks) return;
        be.tickCount = 0;

        if (!(level instanceof ServerLevel sl)) return;

        switch (be.type) {
            case FURNACE -> tickFurnace(sl, pos, be);
            case SENTINEL -> tickSentinel(sl, pos, be);
            case HARVESTER -> tickHarvester(sl, pos, be);
            case GENERATOR -> tickGenerator(sl, pos, be);
        }
    }

    // ─── FURNACE: 熔炼相邻容器 ───
    private static void tickFurnace(ServerLevel sl, BlockPos pos, FunctionalManaBlockEntity be) {
        // 找一个相邻容器
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = sl.getBlockEntity(pos.relative(dir));
            if (!(neighbor instanceof Container container)) continue;
            // 查找可熔炼物品
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack input = container.getItem(i);
                if (input.isEmpty()) continue;
                Optional<SmeltingRecipe> recipe = sl.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, new net.minecraft.world.SimpleContainer(input), sl);
                if (recipe.isEmpty()) continue;
                ItemStack result = recipe.get().getResultItem(sl.registryAccess()).copy();
                if (result.isEmpty()) continue;
                // 扣 mana
                if (!drainFromConduits(sl, pos, be.type.aspect, be.type.manaPerOp)) return;
                // 熔炼
                input.shrink(1);
                // 找另一个容器（或同一容器）放结果，简化：尝试放回同一容器
                ItemStack remaining = result.copy();
                for (int j = 0; j < container.getContainerSize() && !remaining.isEmpty(); j++) {
                    ItemStack slot = container.getItem(j);
                    if (slot.isEmpty()) {
                        container.setItem(j, remaining);
                        remaining = ItemStack.EMPTY;
                    } else if (ItemStack.isSameItemSameTags(slot, remaining)
                            && slot.getCount() + remaining.getCount() <= slot.getMaxStackSize()) {
                        slot.grow(remaining.getCount());
                        remaining = ItemStack.EMPTY;
                    }
                }
                if (!remaining.isEmpty()) {
                    // 容器满，掉落
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    net.minecraft.world.Containers.dropItemStack(sl, pos.getX(), pos.getY() + 1, pos.getZ(), remaining);
                }
                sl.sendParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.05);
                return;
            }
        }
    }

    // ─── SENTINEL: 攻击范围内敌怪 ───
    private static void tickSentinel(ServerLevel sl, BlockPos pos, FunctionalManaBlockEntity be) {
        AABB search = new AABB(pos).inflate(5.0);
        var enemies = sl.getEntitiesOfClass(LivingEntity.class, search,
                e -> e.isAlive() && e instanceof Enemy);
        if (enemies.isEmpty()) return;
        LivingEntity target = enemies.get(0);
        if (!drainFromConduits(sl, pos, be.type.aspect, be.type.manaPerOp)) return;
        target.hurt(sl.damageSources().magic(), 3.0F);
        sl.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 0.5, target.getZ(),
                10, 0.3, 0.3, 0.3, 0.2);
    }

    // ─── HARVESTER: 收割 4 格内成熟作物 ───
    private static void tickHarvester(ServerLevel sl, BlockPos pos, FunctionalManaBlockEntity be) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos cropPos = pos.offset(dx, dy, dz);
                    BlockState cropState = sl.getBlockState(cropPos);
                    if (!(cropState.getBlock() instanceof CropBlock crop)) continue;
                    if (!crop.isMaxAge(cropState)) continue;
                    if (!drainFromConduits(sl, pos, be.type.aspect, be.type.manaPerOp)) return;
                    // 用 fortune 0 破坏作物
                    java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                            cropState, sl, cropPos, sl.getBlockEntity(cropPos));
                    for (ItemStack drop : drops) {
                        net.minecraft.world.Containers.dropItemStack(sl,
                                cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5, drop);
                    }
                    // 重置作物为 age 0
                    sl.setBlock(cropPos, cropState.setValue(BlockStateProperties.AGE_7, 0), 3);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5,
                            6, 0.3, 0.3, 0.3, 0.0);
                    return;
                }
            }
        }
    }

    // ─── GENERATOR: 从 chunk 环境魔力抽取 → 给邻居 conduit 注入 Tainted ───
    private static void tickGenerator(ServerLevel sl, BlockPos pos, FunctionalManaBlockEntity be) {
        ChunkManaSavedData data = ChunkManaSavedData.get(sl);
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(pos);
        float chunkMana = data.getMana(cp);
        if (chunkMana < 50.0F) return; // 不抽空
        // 找一个有空间的邻居 conduit
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = sl.getBlockEntity(pos.relative(dir));
            if (!(neighbor instanceof ManaConduitBlockEntity conduit)) continue;
            int current = conduit.getMana(TypedManaCrystal.ManaAspect.TAINTED);
            if (current >= ManaConduitBlockEntity.MAX_PER_ASPECT) continue;
            int actual = conduit.addMana(TypedManaCrystal.ManaAspect.TAINTED, 2);
            if (actual > 0) {
                data.setMana(cp, chunkMana - 10.0F);
                sl.sendParticles(ParticleTypes.WITCH,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.05);
                return;
            }
        }
    }

    /**
     * 从 6 邻居 conduit BE 中累计抽取 amount 个指定 aspect。
     * 任一邻居成功扣到目标总量 → 返回 true；不足总量 → 回滚并返回 false。
     */
    private static boolean drainFromConduits(ServerLevel sl, BlockPos pos,
                                              TypedManaCrystal.ManaAspect aspect, int amount) {
        if (amount <= 0) return true;
        // 先扫总量
        int available = 0;
        java.util.List<ManaConduitBlockEntity> sources = new java.util.ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = sl.getBlockEntity(pos.relative(dir));
            if (!(neighbor instanceof ManaConduitBlockEntity conduit)) continue;
            available += conduit.getMana(aspect);
            sources.add(conduit);
            if (available >= amount) break;
        }
        if (available < amount) return false;
        // 累计扣除
        int remaining = amount;
        for (ManaConduitBlockEntity src : sources) {
            if (remaining <= 0) break;
            int have = src.getMana(aspect);
            int take = Math.min(remaining, have);
            src.addMana(aspect, -take);
            remaining -= take;
        }
        return true;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("tick_count", tickCount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.tickCount = tag.getInt("tick_count");
    }
}
