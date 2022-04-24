package huige233.transcend.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

public class TextUtils {
    private static final TextFormatting[] fabulousness;
    private static final TextFormatting[] sanic;

    public TextUtils() {
    }

    public static String makeFabulous(String input) {
        return ludicrousFormatting(input, fabulousness, 60.0D, 1, 1);
    }

    public static String makeSANIC(String input) {
        return ludicrousFormatting(input, sanic, 50.0D, 1, 1);
    }

    public static String ludicrousFormatting(String input, TextFormatting[] colours, double delay, int step, int posstep) {
        StringBuilder sb = new StringBuilder(input.length() * 3);
        if (delay <= 0.0D) {
            delay = 0.001D;
        }

        int offset = (int)Math.floor((double) Minecraft.getSystemTime() / delay) % colours.length;

        for(int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int col = (i * posstep + colours.length - offset) % colours.length;
            sb.append(colours[col].toString());
            sb.append(c);
        }

        return sb.toString();
    }

    static {
        fabulousness = new TextFormatting[]{TextFormatting.RED, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.GREEN, TextFormatting.AQUA, TextFormatting.BLUE, TextFormatting.LIGHT_PURPLE};
        sanic = new TextFormatting[]{TextFormatting.BLUE, TextFormatting.BLUE, TextFormatting.BLUE, TextFormatting.BLUE, TextFormatting.WHITE, TextFormatting.BLUE, TextFormatting.WHITE, TextFormatting.WHITE, TextFormatting.BLUE, TextFormatting.WHITE, TextFormatting.WHITE, TextFormatting.BLUE, TextFormatting.RED, TextFormatting.WHITE, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY, TextFormatting.GRAY};
    }
}
