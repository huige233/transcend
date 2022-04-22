package huige233.transcend.init;

import huige233.transcend.blocks.BlockBase;
import huige233.transcend.blocks.bedrockorz;
import huige233.transcend.tileEntity.BlockVirusGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import java.util.ArrayList;
import java.util.List;

public class ModBlock {
    public static final List<Block> BLOCKS = new ArrayList<Block>();
    public static final Block TRANSCEND_BLOCK = new BlockBase("transcend_block", Material.ROCK);
    public static final Block FLAWLESS_BLOCK = new BlockBase("flawless_block", Material.ROCK);
    public static final Block BEDROCK_ORE = new bedrockorz("bedrock_ore", Material.ROCK);
    public static final Block CAST_MACHINE = new BlockVirusGenerator("cast_machine");
}
