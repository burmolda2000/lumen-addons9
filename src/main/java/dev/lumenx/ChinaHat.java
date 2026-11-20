package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

/** China hat: декоративная конусная шляпа над головой (кольца и рёбра с градиентом). */
public final class ChinaHat {
    public static final Module M = new Module("China hat",
            "Decorative hat above the head", Category.VISUALS, false, true);
    static final Setting COLOR  = M.add(Setting.mode("color", "Color", 6, Fx.PAL));
    static final Setting RADIUS = M.add(Setting.num("radius", "Radius", 0.5, 0.3, 1.0, 0.05));
    static final Setting HEIGHT = M.add(Setting.num("height", "Height", 0.35, 0.15, 0.8, 0.05));
    static final Setting SELF   = M.add(Setting.bool("self", "Show yourself", true));
    static final Setting FIRST  = M.add(Setting.bool("first", "Show in first person", false));
    static final Setting OTHERS = M.add(Setting.bool("others", "Other players", true));

    private static final float[] A = new float[3], B = new float[3];

    private ChinaHat() { }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity me, List<Entity> near, float td, long now) {
        if (!M.enabled) return;
        if (SELF.bool && (!Fx.firstPerson(mc) || FIRST.bool)) hat(ctx, me, td, now);
        if (OTHERS.bool) {
            for (Entity e : near) {
                if (!(e instanceof PlayerEntity)) continue;
                if (me.squaredDistanceTo(e) > 40.0 * 40.0) continue;
                hat(ctx, e, td, now);
            }
        }
    }

    private static void hat(DrawContext ctx, Entity e, float td, long now) {
        Box b = Fx.lerped(e, td);
        double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
        double top = b.maxY + 0.02, r = RADIUS.num, h = HEIGHT.num;
        float t = now / 2500f;
        int n = 36;

        // три кольца: основание и два поменьше выше
        for (int k = 0; k < 3; k++) {
            double y = top + h * (k / 3.0);
            double rad = r * (1.0 - k / 3.0);
            boolean prev = false;
            float px = 0, py = 0;
            for (int i = 0; i <= n; i++) {
                double th = 2.0 * Math.PI * i / n;
                boolean ok = Fx.to(cx + Math.cos(th) * rad, y, cz + Math.sin(th) * rad, A);
                if (ok && prev) {
                    int rgb = Fx.color(COLOR.mode, (float) i / n + t + k * 0.1f);
                    Fx.thinLine(ctx, px, py, A[0], A[1], rgb, k == 0 ? 0.95f : 0.6f);
                }
                px = A[0]; py = A[1]; prev = ok;
            }
        }
        // рёбра конуса
        if (!Fx.to(cx, top + h, cz, B)) return;
        for (int i = 0; i < 14; i++) {
            double th = 2.0 * Math.PI * i / 14.0;
            if (Fx.to(cx + Math.cos(th) * r, top, cz + Math.sin(th) * r, A)) {
                int rgb = Fx.color(COLOR.mode, i / 14f + t);
                Fx.thinLine(ctx, A[0], A[1], B[0], B[1], rgb, 0.7f);
            }
        }
        Fx.glowDot(ctx, B[0], B[1], Fx.color(COLOR.mode, t), 0.9f, 1);
    }
}
