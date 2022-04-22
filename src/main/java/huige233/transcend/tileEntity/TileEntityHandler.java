package huige233.transcend.tileEntity;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class TileEntityHandler {
    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(TileEntityVirusGenerator.class,new ResourceLocation("transcend:tileEntityVirusGenerator"));
    }
}
