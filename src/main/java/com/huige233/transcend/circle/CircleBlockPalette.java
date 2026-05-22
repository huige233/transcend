package com.huige233.transcend.circle;

import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import com.huige233.transcend.init.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

/**
 * 法环结构方块调色板。
 *
 * <p>给定 {@link BlockRole} 与 minBlockTier，返回对应的 BlockState 与 ItemStack，
 * 用于建造杖（CircleArchitectWandItem）一键搭建法阵以及生存模式材料消耗。
 *
 * <p>映射规则与 {@link CircleStructurePattern} 中各 tier 所需的 minBlockTier 对齐。
 * 特例：
 * <ul>
 *   <li>CONDUIT 没有 T1 实装方块，T1 fallback 到 T2 的 leyline_conduit_stone。</li>
 *   <li>CATALYST_PLINTH 不分等级，全部使用 catalyst_plinth。</li>
 *   <li>PILLAR_CAP 缺 T5 专属方块，使用 mana_lantern_cap 作为 T5 fallback。</li>
 * </ul>
 */
public final class CircleBlockPalette {

    private CircleBlockPalette() {}

    /**
     * 角色 + 最低方块等级 → BlockState (用于 setBlock)。
     * 输入 minBlockTier 会被夹紧到 [1,5]。
     */
    public static BlockState stateFor(BlockRole role, int minBlockTier) {
        Block block = blockFor(role, minBlockTier);
        return block != null ? block.defaultBlockState() : null;
    }

    /**
     * 角色 + 最低方块等级 → 玩家库存中的物品堆栈 (用于消耗)。
     * 返回的 ItemStack count = 1，调用方负责设置数量。
     */
    public static ItemStack requiredStackFor(BlockRole role, int minBlockTier) {
        Block block = blockFor(role, minBlockTier);
        return block != null ? new ItemStack(block.asItem()) : ItemStack.EMPTY;
    }

    /**
     * 内部统一映射：role + tier → Block。
     */
    private static Block blockFor(BlockRole role, int minBlockTier) {
        int tier = Math.max(1, Math.min(5, minBlockTier));
        return switch (role) {
            case FOUNDATION -> resolve(switch (tier) {
                case 1 -> ModBlocks.ANCIENT_CIRCLE_STONE;
                case 2 -> ModBlocks.AWAKENED_CIRCLE_STONE;
                case 3 -> ModBlocks.ASTRAL_CIRCLE_STONE;
                case 4 -> ModBlocks.NEXUS_CIRCLE_STONE;
                default -> ModBlocks.PRIMORDIAL_CIRCLE_STONE;
            });
            case RUNE -> resolve(switch (tier) {
                case 1 -> ModBlocks.LESSER_RUNE_STONE;
                case 2 -> ModBlocks.AWAKENED_RUNE_STONE;
                case 3 -> ModBlocks.GREATER_RUNE_STONE;
                case 4 -> ModBlocks.ARCHON_RUNE_STONE;
                default -> ModBlocks.PRIMORDIAL_RUNE_STONE;
            });
            case CONDUIT -> resolve(switch (tier) {
                case 1, 2 -> ModBlocks.LEYLINE_CONDUIT_STONE;
                case 3 -> ModBlocks.AETHER_CHANNEL_MARKER;
                case 4 -> ModBlocks.NEXUS_CONDUIT_GATE;
                default -> ModBlocks.PRIMORDIAL_CONDUIT_GATE;
            });
            case CATALYST_PLINTH -> resolve(ModBlocks.CATALYST_PLINTH);
            case PILLAR -> resolve(switch (tier) {
                case 1, 2, 3 -> ModBlocks.RUNIC_PILLAR;
                case 4 -> ModBlocks.NEXUS_OBELISK;
                default -> ModBlocks.PRIMORDIAL_PYLON;
            });
            case PILLAR_CAP -> resolve(switch (tier) {
                case 1, 2, 3 -> ModBlocks.ASTRAL_CAPSTONE;
                // T4 与 T5 都使用 mana_lantern_cap (T5 暂无专属方块)
                default -> ModBlocks.MANA_LANTERN_CAP;
            });
            case CORE -> resolve(switch (tier) {
                case 1 -> ModBlocks.CIRCLE_CORE_DORMANT;
                case 2 -> ModBlocks.CIRCLE_CORE_WELLSPRING;
                case 3 -> ModBlocks.CIRCLE_CORE_SANCTUARY;
                case 4 -> ModBlocks.CIRCLE_CORE_DOMINION;
                default -> ModBlocks.CIRCLE_CORE_PRIMORDIAL;
            });
        };
    }

    private static Block resolve(RegistryObject<Block> ro) {
        return ro != null ? ro.get() : null;
    }
}
