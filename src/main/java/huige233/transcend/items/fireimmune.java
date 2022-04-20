package huige233.transcend.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;

public class fireimmune extends EntityItem {
    public fireimmune(World world, Entity location, ItemStack stack) {
        this(world, location.posX, location.posY, location.posZ, stack);
        this.setPickupDelay(0);
        this.motionX = location.motionX;
        this.motionY = location.motionY;
        this.motionZ = location.motionZ;
        this.setItem(stack);
    }
    public fireimmune(World world, double x, double y, double z, ItemStack itemstack) {
        super(world, x, y, z, itemstack);
        this.setItem(itemstack);
    }

    public fireimmune(World world, double x, double y, double z) {
        super(world, x, y, z);
        this.isImmuneToFire = true;
    }

    public fireimmune(World world) {
        super(world);
        isImmuneToFire = true;
    }


    protected void dealFireDamage(int damage) {
    }

    @Override
    public boolean attackEntityFrom(@Nonnull DamageSource source, float amount) {
        if (source.getDamageType().equals(DamageSource.OUT_OF_WORLD.damageType)) {
            return true;
        }
        // prevent any damage besides out of world
        return false;
    }

    public static class EventHandler {

        public static final EventHandler instance = new EventHandler();

        private EventHandler() {
        }

        @SubscribeEvent
        public void onExpire(ItemExpireEvent event) {
            if (event.getEntityItem() instanceof fireimmune) {
                event.setCanceled(true);
            }
        }
    }
}
