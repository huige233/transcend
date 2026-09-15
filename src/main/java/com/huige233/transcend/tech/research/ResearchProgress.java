package com.huige233.transcend.tech.research;

import net.minecraft.nbt.CompoundTag;
import java.math.BigInteger;
import java.util.*;


/** 保存玩家已完成与进行中的研究、已付研究点和耗时，并在供点充足时推进完成判定。 */
public final class ResearchProgress {
    private final Set<String> completed = new HashSet<>();
    private String active;
    private BigInteger paid = BigInteger.ZERO;
    private long ticks;
    public Set<String> completed() { return Set.copyOf(completed); }
    public String active() { return active; }
    public BigInteger paid() { return paid; }
    public long ticks() { return ticks; }
    public boolean start(String id) { if (!ResearchService.canStart(completed,id,active)) return false; active=id; paid=BigInteger.ZERO; ticks=0; return true; }
    public boolean canStartOrResume(String id, BigInteger balance) {
        if (active != null) return active.equals(id);
        return balance != null && balance.signum() > 0 && ResearchService.canStart(completed, id, null);
    }
    
    public boolean advance(ResearchEnergyAccount account) {
        if (active == null) return false;
        BigInteger accepted = addPoints(account.points().min(requiredEnergyPerTick()));
        account.spend(accepted);
        if (accepted.signum() == 0 && remainingPoints().signum() > 0) return false;
        tick();
        return true;
    }
    public BigInteger addPoints(BigInteger amount) {
        if (active==null || amount==null || amount.signum()<=0) return BigInteger.ZERO;
        BigInteger accepted=amount.min(ResearchService.remaining(paid,active)); paid=paid.add(accepted); return accepted;
    }
    
    @Deprecated public void addEnergy(BigInteger amount) { addPoints(amount); }
    public BigInteger remainingPoints() { return ResearchService.remaining(paid,active); }
    public boolean tick() {
        if (active==null) return false;
        ResearchNode n=ResearchRegistry.get(active); if(n==null){active=null;paid=BigInteger.ZERO;ticks=0;return false;}
        ticks = ticks == Long.MAX_VALUE ? Long.MAX_VALUE : Math.min(n.durationTicks(), ticks + 1);
        if(paid.compareTo(n.cost())>=0 && ticks>=n.durationTicks()){completed.add(active);active=null;paid=BigInteger.ZERO;ticks=0;return true;} return false;
    }
    public BigInteger requiredEnergyPerTick() { if(active==null)return BigInteger.ZERO; ResearchNode n=ResearchRegistry.get(active); if(n==null)return BigInteger.ZERO; long left=Math.max(1,n.durationTicks()-ticks); return remainingPoints().add(BigInteger.valueOf(left-1)).divide(BigInteger.valueOf(left)); }
    public void load(CompoundTag tag) {
        completed.clear(); List<String> raw=new ArrayList<>(); var list=tag.getList("Completed",8); for(int i=0;i<Math.min(list.size(),1024);i++){String id=list.getString(i);if(ResearchRegistry.get(id)!=null)raw.add(id);}
        boolean changed; do {changed=false; for(String id:raw) if(!completed.contains(id)&&ResearchService.isUnlocked(completed,id)){completed.add(id);changed=true;}} while(changed);
        String id=tag.getString("Active"); active=ResearchService.canStart(completed,id.isEmpty()?null:id,null)?id:null; paid=active==null?BigInteger.ZERO:parse(tag.getString("Paid")).min(ResearchRegistry.get(active).cost()); ticks=active==null?0:Math.min(ResearchRegistry.get(active).durationTicks(),Math.max(0,tag.getLong("Ticks")));
    }
    public void save(CompoundTag tag) { var list=new net.minecraft.nbt.ListTag(); completed.stream().sorted().forEach(id->list.add(net.minecraft.nbt.StringTag.valueOf(id))); tag.put("Completed",list);tag.putString("Active",active==null?"":active);tag.putString("Paid",paid.toString());tag.putLong("Ticks",ticks); }
    private static BigInteger parse(String v){return v!=null&&v.matches("[0-9]{1,64}")?new BigInteger(v):BigInteger.ZERO;}
}
