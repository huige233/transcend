package com.huige233.transcend.circle;

import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import com.huige233.transcend.init.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

public final class CircleBlockPalette {

    private CircleBlockPalette() {}

    public static BlockState stateFor(BlockRole role, int minBlockTier) {
        Block block = blockFor(role, minBlockTier);
        return block != null ? block.defaultBlockState() : null;
    }

    public static ItemStack requiredStackFor(BlockRole role, int minBlockTier) {
        Block block = blockFor(role, minBlockTier);
        return block != null ? new ItemStack(block.asItem()) : ItemStack.EMPTY;
    }

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
