package com.huige233.transcend.util;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** 验证机器数值经有符号短整型数据包传输后无损恢复，并检查产量边界和菜单同步约定。 */
class MachineDataValuesTest {
    @Test void intWordsSurviveSignedShortPackets() {
        for (int value : new int[]{0, 32768, 65535, 65536, 100000, 16000000, Integer.MAX_VALUE}) {
            int[] words = {(short) value, (short) (value >>> 16)};
            assertEquals(value, MachineDataValues.readInt(i -> words[i], 0));
        }
    }

    @Test void bigWordsPreserveFullMachineRange() {
        for (BigInteger value : new BigInteger[]{BigInteger.ZERO, BigInteger.TEN.pow(40), BigInteger.TEN.pow(64).subtract(BigInteger.ONE)}) {
            assertEquals(value, MachineDataValues.readBig(i -> (short) MachineDataValues.word(value, i), 0));
        }
    }

    @Test void productionCannotOverdrawFinalRemainder() {
        assertEquals(7, MachineDataValues.production(BigInteger.valueOf(7), 100000, 16000000));
        assertEquals(2, MachineDataValues.production(BigInteger.TEN, 100000, 2));
        assertEquals(0, MachineDataValues.production(BigInteger.valueOf(-1), 100000, 100));
        assertEquals(0, MachineDataValues.production(BigInteger.TEN, 100000, -1));
    }

    @Test void menuUsesLittleEndianOutputAndSynchronizedCharge() throws Exception {
        String menu = Files.readString(Path.of("src/main/java/com/huige233/transcend/menu/MiniUniverseGeneratorMenu.java"));
        assertTrue(menu.contains("private static final int OUTPUT = 2;"));
        assertTrue(menu.contains("private static final int OUTPUT_SLOT = 1;"));
        String data = Files.readString(Path.of("src/main/java/com/huige233/transcend/menu/MiniUniverseGeneratorData.java"));
        assertTrue(data.contains("if (index == 2) return output & 0xffff;"));
        assertTrue(data.contains("if (index == 3) return (output >>> 16) & 0xffff;"));
        assertTrue(menu.contains("MachineDataValues.readBig(data::get, CHARGED)"));
        assertTrue(menu.contains("new SlotItemHandler(machineItems, INPUT_SLOT, SLOT_X, SLOT_Y)"));
        String screen = Files.readString(Path.of("src/main/java/com/huige233/transcend/client/MiniUniverseGeneratorScreen.java"));
        assertTrue(screen.contains("MachinePanelStyle.slot(g, x, y, menu.slots.get(i)"),
                "Slot wells must follow actual menu slot coordinates");
    }
}
