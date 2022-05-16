package huige233.transcend.init;

import huige233.transcend.blocks.BlockBase;
import huige233.transcend.blocks.bedrockorz;
import huige233.transcend.blocks.voidblock;
import huige233.transcend.tileEntity.BlockBedRockCollector;
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
    public static final Block VOIDBLOCK = new voidblock("void_block", Material.ROCK);
    public static final Block BEDROCK_COLLECTOR = new BlockBedRockCollector("bedrock_collector");
    public static final Block CAST_MACHINE = new BlockVirusGenerator("cast_machine");
    public static final Block NETHER_STAR_BLOCK = new BlockBase("nether_star_block", Material.ROCK).setHardness(5.0F);
}
