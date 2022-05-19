package huige233.transcned.items.tool;

import huige233.transcned.Main;
import huige233.transcned.items.ItemTier;
import huige233.transcned.utl.ItemNBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.core.jmx.Server;

import javax.swing.plaf.ActionMapUIResource;

public class ItemSword extends SwordItem {
    public ItemSword() {
        super(ItemTier.TranscendToolTier,9999,1,new Item.Properties().tab(Main.TRANSCEND_TAB));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if(entity instanceof LivingEntity) {
            LivingEntity t = (LivingEntity) entity;
            //t.remove(Entity.RemovalReason.KILLED);
            if(ItemNBTHelper.getBoolean(stack,"Destruction",false)){
                entity.setInvulnerable(false);
                t.deathTime=20;
            }
            t.die(DamageSource.OUT_OF_WORLD);
            t.setHealth(0);
            return true;
        }
        return false;
    }
    @SubscribeEvent
    public static InteractionResult onItemRightClick(RightClickItem event){
        InteractionHand hand = event.getHand();
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        CompoundTag tag = stack.getTag();
        PlayerInteractEvent.RightClickItem evt = new PlayerInteractEvent.RightClickItem(player, hand);
        boolean destruction = ItemNBTHelper.getBoolean(stack,"Destruction",false);
        if(tag == null) {
            ItemNBTHelper.setBoolean(stack,"Destruction",false);
        } else{
            if(!destruction) {
                ItemNBTHelper.setBoolean(stack,"Destruction",true);
            } else {
                ItemNBTHelper.setBoolean(stack,"Destruction",false);
            }
        }
        return evt.isCanceled() ? evt.getCancellationResult() : null;
    }
}
