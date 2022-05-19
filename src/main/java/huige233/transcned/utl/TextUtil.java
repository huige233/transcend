package huige233.transcned.utl;

import net.minecraft.ChatFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TextUtil {
    private static final ChatFormatting[] fabulousness;
    private static final ChatFormatting[] sanic;

    public static String makeFabulous(String input) {
        return ludicrousFormatting(input, fabulousness, 5.0D, 1, 1);
    }

    public static String makeSANIC(String input) {
        return ludicrousFormatting(input, sanic, 4.0D, 1, 1);
    }

    static int ticks;
    static long currentCount;

    static final int startingCount = 449;

    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        ticks++;
        if (ticks < startingCount) currentCount = startingCount;
        else if (ticks > startingCount && ticks < 5000) currentCount = ticks;
        else currentCount += Math.random() * currentCount / 100;
    }

    public static String modCounter(){
        return (currentCount < Integer.MAX_VALUE) ? String.valueOf(currentCount) : makeFabulous("Infinity");
    }

    public static String ludicrousFormatting(String input, ChatFormatting[] colours, double delay, int step, int posstep) {
        StringBuilder sb = new StringBuilder(input.length() * 3);
        if (delay <= 0.0D) {
            delay = 0.001D;
        }

        int offset = (int)Math.floor(ticks / delay) % colours.length;

        for(int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int col = (i * posstep + colours.length - offset) % colours.length;
            sb.append(colours[col].toString());
            sb.append(c);
        }

        return sb.toString();
    }

    static {
        fabulousness = new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW,
                ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE};
        sanic = new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.BLUE, ChatFormatting.BLUE, ChatFormatting.BLUE,
                ChatFormatting.WHITE, ChatFormatting.BLUE, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.BLUE,
                ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.WHITE,
                ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY,
                ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY,
                ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.GRAY};
    }
}
