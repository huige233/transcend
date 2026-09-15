package com.huige233.transcend.util;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import static net.minecraft.ChatFormatting.*;


/** 按当前时间和字符位置轮换格式颜色，生成逐字流动的彩虹文本。 */
public class TextUtils {
    private static final ChatFormatting[] fabulousness = new ChatFormatting[]{RED, GOLD, YELLOW, GREEN, AQUA, BLUE, LIGHT_PURPLE};

    public static String makeFabulous(String input) {
        return ludicrousFormatting(input, fabulousness, 240.0, 1, 1);
    }

    public static String ludicrousFormatting(String input, ChatFormatting[] colours, double delay, int step, int posstep) {
        StringBuilder sb = new StringBuilder(input.length() * 3);
        if (delay <= 0) {
            delay = 0.001;
        }

        int offset = (int) Math.floor(Util.getMillis() / delay) % colours.length;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            int col = ((i * posstep) + colours.length - offset) % colours.length;

            sb.append(colours[col].toString());
            sb.append(c);
        }

        return sb.toString();
    }
}
