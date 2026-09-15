package com.huige233.transcend.items.curio;

import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.type.capability.ICurioItem;


/** 将超越饰品接入 Curios 装备接口，供外部事件处理器识别并授予防护。 */
public class TranscendCurio extends Item implements ICurioItem {
    public TranscendCurio(Properties properties) {
        super(properties);
    }
}
