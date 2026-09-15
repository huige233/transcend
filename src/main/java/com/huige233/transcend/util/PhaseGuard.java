package com.huige233.transcend.util;

import com.huige233.transcend.items.armor.TranscendArmor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

   
                                                                
                                                             
                           
                                                                      
  
                                                    
   
/** 维护相位锁定、自主移动上下文和飞行权限，并在飞行穿越时恢复无碰撞与无重力状态。 */
public final class PhaseGuard {

    private PhaseGuard() {
    }

    
    private static final Set<java.util.UUID> LOCKED = new CopyOnWriteArraySet<>();
    
    private static final ThreadLocal<Map<Player, Integer>> SELF = ThreadLocal.withInitial(IdentityHashMap::new);
    
    private static final ThreadLocal<Player> MOVING = new ThreadLocal<>();
    
    public static final String PHASE_TRAVERSAL_TAG = "transcend_phase_traversal";
    
    public static final String NBT_PHASE_FLIGHT = "transcend_phase_flight";

    

    public static void lock(Player player) {
        if (player != null && !player.level().isClientSide) {
            LOCKED.add(player.getUUID());
        }
    }

    public static void unlock(Player player) {
        if (player != null) {
            LOCKED.remove(player.getUUID());
        }
    }

    public static void clearAll(ServerPlayer player) {
        if (player != null) {
            unlock(player);
            SELF.get().remove(player);
            if (MOVING.get() == player) MOVING.remove();
        }
    }

    
    public static boolean isLocked(Player player) {
        return player != null && LOCKED.contains(player.getUUID());
    }

    
    public static boolean blocksForces(Player player) {
        return isLocked(player) && !isSelfMovement(player);
    }

    
    public static boolean blocksTeleports(Player player) {
        return isLocked(player) && !isSelfTeleport(player);
    }

    

    
    public static boolean isSelfMovement(Player player) {
        if (player == null) return false;
        if (SELF.get().getOrDefault(player, 0) > 0) return true;
        return isCurrentMovePacket(player);
    }

    
    public static boolean isSelfTeleport(Player player) {
        return isSelfMovement(player);
    }

    
    public static boolean isInnerMovePositionUpdate() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(f -> f.limit(20)
                .anyMatch(frame -> frame.getDeclaringClass() == net.minecraft.world.entity.Entity.class
                        && "move".equals(frame.getMethodName())));
    }

    public static void beginSelf(Player player) {
        if (player == null) return;
        SELF.get().merge(player, 1, Integer::sum);
    }

    public static void endSelf(Player player) {
        if (player == null) return;
        Map<Player, Integer> depths = SELF.get();
        int next = depths.getOrDefault(player, 0) - 1;
        if (next <= 0) {
            depths.remove(player);
            if (depths.isEmpty()) SELF.remove();
        } else {
            depths.put(player, next);
        }
    }

    public static void runAsSelf(Player player, Runnable action) {
        beginSelf(player);
        try {
            action.run();
        } finally {
            endSelf(player);
        }
    }

    public static void beginMovePacket(Player player) {
        if (player != null && !player.level().isClientSide) {
            MOVING.set(player);
        }
    }

    public static void endMovePacket(Player player) {
        if (MOVING.get() == player) {
            MOVING.remove();
        }
    }

    private static boolean isCurrentMovePacket(Player player) {
        if (!(player instanceof ServerPlayer) || MOVING.get() != player) return false;
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(f -> f.limit(20)
                .anyMatch(frame -> frame.getDeclaringClass()
                                == net.minecraft.server.network.ServerGamePacketListenerImpl.class
                        && "handleMovePlayer".equals(frame.getMethodName())));
    }

    
    
    private static final java.util.Set<java.util.UUID> FLIGHT_ACTIVE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void setFlightActive(Player player, boolean active) {
        if (player == null) return;
        if (active) FLIGHT_ACTIVE.add(player.getUUID());
        else FLIGHT_ACTIVE.remove(player.getUUID());
    }

    public static boolean isFlightActive(Player player) {
        return player != null && FLIGHT_ACTIVE.contains(player.getUUID());
    }

                                                            
                                                            
                                               
                                                                                          
                                                                  
    public static void syncPhaseFlight(Player player) {
        if (player == null || player.level().isClientSide) return;
        boolean active = isFlightActive(player);
        boolean changed = false;
        if (active) {
            
            if (!player.getAbilities().mayfly) { player.getAbilities().mayfly = true; changed = true; }
        } else if (!player.isCreative() && !player.isSpectator()) {
            if (player.getAbilities().mayfly) { player.getAbilities().mayfly = false; changed = true; }
            if (player.getAbilities().flying) { player.getAbilities().flying = false; changed = true; }
        }
        
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    

                                                        
                                                                 
    public static boolean isPhaseEnabled(Player player) {
        return PhaseFlightState.isEnabled(player);
    }

    
    public static boolean isTraversing(Player player) {
        return player != null && player.getPersistentData().getBoolean(PHASE_TRAVERSAL_TAG);
    }

    
    public static void beginTraversal(Player player) {
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setOnGround(false);
        player.fallDistance = 0.0F;
        player.getPersistentData().putBoolean(PHASE_TRAVERSAL_TAG, true);
    }

    
    public static void endTraversal(Player player) {
        player.noPhysics = player.isSpectator();
        player.setNoGravity(player.isSpectator());
        player.getPersistentData().remove(PHASE_TRAVERSAL_TAG);
    }

                                                                       
                                                                      
                                                       
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null) return;
        enforcePhaseForTick(player);
    }

                                                     
                                                    
                                                                       
    public static boolean isPhaseActiveForMove(Player player) {
        if (player == null || player.isSpectator()) return false;
        if (!TranscendArmor.isPhaseFlightEnabled(player)) return false;
        return player.getAbilities().flying;
    }

                                                                                                 
                                                             
                                                                 
    public static void enforcePhaseForTick(Player player) {
        if (player == null) return;
        
        
        
        
        
        
        
        boolean enabled = TranscendArmor.isPhaseFlightEnabled(player);
        boolean flying = player.getAbilities().flying;
        if (enabled && flying && !player.isSpectator()) {
            beginTraversal(player);
        } else {
            
            endTraversal(player);
        }
    }

    
    public static void maintain(Player player) {
        if (player == null || player.level().isClientSide) return;
        boolean enabled = TranscendArmor.isPhaseFlightEnabled(player);
        
        boolean flying = player.getAbilities().flying;

        if (enabled && flying) {
            
            if (!isTraversing(player) && !player.isSpectator()) {
                beginTraversal(player);
            }
        } else {
            if (isTraversing(player)) {
                endTraversal(player);
            }
        }
    }
}