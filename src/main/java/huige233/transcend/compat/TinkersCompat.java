package huige233.transcend.compat;

import huige233.transcend.compat.tinkers.TiCConfig;
public class TinkersCompat {
    public TinkersCompat(){
        init();
    }
    public static boolean enabled = false;

    public void init() {
        if(enabled){
            TiCConfig.TiCMaterials.setup();
            TiCConfig.TiCMaterials.setRenderInfo();
        }
    }
}