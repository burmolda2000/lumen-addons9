package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

/** Hit color: окрашивает цель после вашего попадания (заливка и/или контур по её хитбоксу). */
public final class HitColor {
    public static final Module M = new Module("Hit color",
            "Tints a player after you hit them", Category.VISUALS, false, true);
    static final Setting COLOR      = M.add(Setting.mode("color", "Color", 2, Fx.PAL));
    static final Setting CRIT_COLOR = M.add(Setting.mode("crit_color", "Crit color", 1, Fx.PAL));
    static final Setting STYLE      = M.add(Setting.mode("style", "Style", 0, "Tint", "Outline", "Both"));
    static final Setting TIME       = M.add(Setting.num("time", "Duration (sec)", 0.5, 0.1, 2, 0.1));
    static final Setting ALPHA      = M.add(Setting.num("alpha", "Strength (%)", 35, 5, 80, 5));

    private static LivingEntity target;
    private static long t0;
    private static boolean crit;

    private HitColor() { }

    static void onHit(LivingEntity e, boolean c, long now) {
        if (!M.enabled) return;
        target = e; t0 = now; crit = c;
    }

    static void draw(DrawContext ctx, long now, float td) {
        if (!M.enabled || target == null) return;
        float age = (now - t0) / (float) (TIME.num * 1000.0);
        if (age >= 1f || !target.isAlive()) { target = null; return; }

        Box b = Fx.lerped(target, td);
        float[][] pts = Fx.PTS;
        if (!Fx.projectBox(b, pts)) return;
        Fx.rect(pts, Fx.RECT);
        int rgb = Fx.color(crit ? CRIT_COLOR.mode : COLOR.mode, now / 1500f);
        float a = 1f - age;
        int st = STYLE.mode;
        if (st != 1) {
            Fx.fill(ctx, (int) Fx.RECT[0], (int) Fx.RECT[1], (int) Fx.RECT[2], (int) Fx.RECT[3],
                    rgb, a * (float) ALPHA.num / 100f);
        }
        if (st != 0) Fx.boxEdges(ctx, pts, rgb, a);
    }
}
