package com.huige233.transcend.menu;

import com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity;
import com.huige233.transcend.util.MachineDataValues;
import net.minecraft.world.inventory.ContainerData;


/** 将微型宇宙发电机的缓冲能量、大整数储量、阶段和超频状态编码为菜单同步数据。 */
final class MiniUniverseGeneratorData implements ContainerData {
    private final MiniUniverseGeneratorBlockEntity be;
    MiniUniverseGeneratorData(MiniUniverseGeneratorBlockEntity be) { this.be = be; }
    @Override public int get(int index) {
        int input = be.inputEnergy(), output = be.outputEnergy();
        if (index == 0) return input & 0xffff;
        if (index == 1) return (input >>> 16) & 0xffff;
        if (index == 2) return output & 0xffff;
        if (index == 3) return (output >>> 16) & 0xffff;
        if (index == 4) return be.phaseId();
        int charged = 5 + MachineDataValues.BIG_WORDS;
        int status = charged + MachineDataValues.BIG_WORDS;
        if (index >= charged && index < status) return MachineDataValues.word(be.chargedAmount(), index - charged);
        if (index == status) return be.statusId();
        if (index == status + 1) return be.overclocked() ? 1 : 0;
        if (index >= 5) return MachineDataValues.word(be.remainingAmount(), index - 5);
        return 0;
    }
    @Override public void set(int index, int value) { }
    @Override public int getCount() { return MiniUniverseGeneratorMenu.DATA_COUNT; }
}
