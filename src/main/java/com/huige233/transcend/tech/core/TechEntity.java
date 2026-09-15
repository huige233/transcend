package com.huige233.transcend.tech.core;

import com.huige233.transcend.tech.attribute.AttributeContainer;

   
                                     
  
                                      
                                                   
                  
  
                                                                
                                                     
   
/** 为持有科技属性的对象提供统一属性容器访问及逐刻临时修饰器清理入口。 */
public interface TechEntity {

    
    AttributeContainer techAttributes();

       
                       
      
                                                         
       
    default void techTick(long currentGameTime) {
        techAttributes().tickCleanup(currentGameTime);
    }
}
