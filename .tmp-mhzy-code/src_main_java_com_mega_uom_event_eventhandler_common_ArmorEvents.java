package com.mega.uom.event.eventhandler.common;

import com.mega.uom.ModSource;
import com.mega.uom.common.items.ModArmorMaterials;
import com.mega.uom.common.items.armor.IArmor;
import com.mega.uom.common.items.armor.ModArmorItem;
import com.mega.uom.util.itf.LivingEntityEC;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = ModSource.MODID)
public class ArmorEvents {
    public static @Nullable ArmorMaterial getArmorSet(LivingEntity living) {
        return ((LivingEntityEC) living).uom$livingECData().getCurrentArmorSet();
    }

}
