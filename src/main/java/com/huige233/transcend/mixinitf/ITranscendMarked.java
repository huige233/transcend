package com.huige233.transcend.mixinitf;


/** 约定实体终结标记的设置、查询和清除接口，使击杀流程能够覆盖装备保护。 */
public interface ITranscendMarked {
    void transcend$mark();
    boolean transcend$isMarked();
    void transcend$unmark();
}
