package com.huige233.transcend.tech.api;

   
                  
  
                                                        
                                   
   
/** 约定科技设备长整型能量的容量查询及支持模拟的充放电操作。 */
public interface ITechEnergy {
    long stored();
    long capacity();
    long receive(long amount, boolean simulate);
    long extract(long amount, boolean simulate);
}
