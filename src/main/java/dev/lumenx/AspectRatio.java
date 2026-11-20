package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Aspect ratio: растягивает 3D-картинку под другое соотношение сторон (как «растяжка» в PvP). */
public final class AspectRatio {
    static final String[] PRESETS = { "Custom", "4:3", "5:4", "16:10", "1:1", "3:2", "21:9", "32:9" };
    private static final double[] VALUES = { 0.0, 4.0 / 3.0, 5.0 / 4.0, 16.0 / 10.0, 1.0, 1.5, 21.0 / 9.0, 32.0 / 9.0 };

    public static final Module M = new Module("Aspect ratio",
            "Stretch the 3D view to another aspect ratio", Category.VISUALS, false, true);
    static final Setting PRESET = M.add(Setting.mode("preset", "Preset", 1, PRESETS));
    static final Setting RATIO  = M.add(Setting.num("ratio", "Custom ratio", 1.33, 0.5, 3.0, 0.01));

    private static volatile long lastApply;
    private static long enabledSince;
    private static boolean warned;

    private AspectRatio() { }

    static double target() {
        int i = Math.max(0, Math.min(PRESETS.length - 1, PRESET.mode));
        return i == 0 ? RATIO.num : VALUES[i];
    }

    /** Миксин вызывает это каждый раз, когда строится матрица проекции мира. */
    public static float apply(float aspect) {
        lastApply = Util.getMeasuringTimeMs();
        if (!M.enabled) return aspect;
        MinecraftClient mc = MinecraftClient.getInstance();
        float win = (float) mc.getWindow().getFramebufferWidth() / (float) Math.max(1, mc.getWindow().getFramebufferHeight());
        if (Math.abs(aspect - win) > win * 0.03f) return aspect;   // не трогаем нестандартные проекции
        return (float) target();
    }

    static boolean hooked() {
        return Util.getMeasuringTimeMs() - lastApply < 1500L;
    }

    /** Во сколько раз нужно растянуть ширину относительно окна (для выравнивания наших оверлеев). */
    static float factor(int w, int h) {
        if (!M.enabled || !hooked() || h <= 0) return 1f;
        double f = target() / ((double) w / (double) h);
        return (float) Math.max(0.3, Math.min(3.0, f));
    }

    /** Если хук не сработал (другая версия рендера), один раз сообщаем об этом. */
    static void tick(ClientPlayerEntity p, long now) {
        if (!M.enabled) { enabledSince = 0; warned = false; return; }
        if (enabledSince == 0) enabledSince = now;
        if (!warned && now - enabledSince > 2500 && !hooked()) {
            warned = true;
            p.sendMessage(Text.literal("Aspect ratio: hook is not active in this build"), false);
        }
    }
}
