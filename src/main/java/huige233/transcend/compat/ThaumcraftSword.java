package huige233.transcend.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType;

import java.util.Random;

public class ThaumcraftSword{
    public static boolean damageEntity(EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            EntityPlayer t = (EntityPlayer) target;
            ThaumcraftApi.internalMethods.addWarpToPlayer(t, 100, EnumWarpType.NORMAL);
            ThaumcraftApi.internalMethods.addWarpToPlayer(t, 100, EnumWarpType.PERMANENT);
            ThaumcraftApi.internalMethods.addWarpToPlayer(t, 100, EnumWarpType.TEMPORARY);
        }
        return true;
    }
    public static void warpsword(EntityLivingBase target) {
        if(target instanceof EntityPlayer) {
            EntityPlayer t = (EntityPlayer) target;
            Random r = target.world.rand;
            if(r.nextDouble() < 0.5) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(t, 3, EnumWarpType.NORMAL);
            }
        }
    }
}
