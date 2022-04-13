package huige233.transcend.lib;

import net.minecraft.util.DamageSource;
public class TranscendDamageSources {
    public static final DamageSource FLAWLESS = (new DamageSource( "flawless" ) ).setDamageIsAbsolute().setDamageBypassesArmor().setDamageIsAbsolute().setDamageAllowedInCreativeMode().setMagicDamage().setExplosion();

    public static class DamageFLAWLESS extends DamageSource {
        public DamageFLAWLESS()
        {
            super("flawless");
            setDamageAllowedInCreativeMode();
            setDamageBypassesArmor();
            setDamageIsAbsolute();
            setMagicDamage();
            setExplosion();
        }
    }
    public boolean isUnblockable()
    {
        return(true);
    }
}
