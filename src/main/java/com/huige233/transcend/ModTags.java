package com.huige233.transcend;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;


/** 定义普通、史诗和超越工具对应的方块采掘等级标签。 */
public class ModTags {
    public static final TagKey<Block> NEEDS_NORMAL_TOOL = BlockTags.create(Transcend.rl("needs_normal_tool"));
    public static final TagKey<Block> NEEDS_EPICC_TOOL = BlockTags.create(Transcend.rl("needs_epicc_tool"));
    public static final TagKey<Block> NEEDS_TRANSCEND_TOOL = BlockTags.create(Transcend.rl("needs_transcend_tool"));

}
