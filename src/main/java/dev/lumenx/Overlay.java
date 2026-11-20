package dev.lumenx;

import dev.lumen.util.Proj;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Общая система частиц, рисуемых поверх экрана через проекцию мировых координат. */
final class Overlay {
    private Overlay() { }

    static final int DOT = 0, STAR = 1, SHARD = 2, COMET = 3, TWINKLE = 4;

    static final class P {
        double x, y, z, vx, vy, vz;
        float gravity, life, size, phase;
        int rgb, kind;
        long born;
    }

    static final class Ring {
        double x, y, z;
        float r0, r1, life;
        int rgb;
        long born;
    }

    private static final List<P> PS = new ArrayList<>();
    private static final List<Ring> RS = new ArrayList<>();
    private static long last;
    private static final float[] A = new float[3], B = new float[3];

    static P add(int kind, double x, double y, double z, double vx, double vy, double vz,
                 float gravity, float life, float size, int rgb) {
        if (PS.size() > 1500) return null;
        P p = new P();
        p.kind = kind; p.x = x; p.y = y; p.z = z; p.vx = vx; p.vy = vy; p.vz = vz;
        p.gravity = gravity; p.life = Math.max(0.05f, life); p.size = size; p.rgb = rgb;
        p.born = Util.getMeasuringTimeMs();
        p.phase = ThreadLocalRandom.current().nextFloat() * 6.2832f;
        PS.add(p);
        return p;
    }

    static void ring(double x, double y, double z, float r0, float r1, float life, int rgb) {
        if (RS.size() > 60) return;
        Ring r = new Ring();
        r.x = x; r.y = y; r.z = z; r.r0 = r0; r.r1 = r1; r.life = Math.max(0.05f, life); r.rgb = rgb;
        r.born = Util.getMeasuringTimeMs();
        RS.add(r);
    }

    static void render(DrawContext ctx, long now) {
        float dt = last == 0 ? 0f : MathHelper.clamp((now - last) / 1000f, 0f, 0.1f);
        last = now;

        for (Iterator<P> it = PS.iterator(); it.hasNext(); ) {
            P p = it.next();
            float age = (now - p.born) / 1000f / p.life;
            if (age >= 1f) { it.remove(); continue; }
            p.vy -= p.gravity * dt;
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
            float alpha = 1f - age;
            if (!Fx.to(p.x, p.y, p.z, A)) continue;
            int r = Math.max(0, Math.round(p.size));
            switch (p.kind) {
                case DOT -> Fx.glowDot(ctx, A[0], A[1], p.rgb, alpha, r);
                case STAR -> star(ctx, A[0], A[1], p.rgb, alpha, p.size);
                case SHARD -> {
                    if (Fx.to(p.x - p.vx * 0.06, p.y - p.vy * 0.06, p.z - p.vz * 0.06, B)) {
                        Fx.thinLine(ctx, A[0], A[1], B[0], B[1], p.rgb, alpha);
                    }
                    Fx.glowDot(ctx, A[0], A[1], p.rgb, alpha, Math.max(0, r - 1));
                }
                case COMET -> {
                    for (int i = 1; i <= 7; i++) {
                        double k = 0.05 * i;
                        if (Fx.to(p.x - p.vx * k, p.y - p.vy * k, p.z - p.vz * k, B)) {
                            Fx.glowDot(ctx, B[0], B[1], p.rgb, alpha * (1f - i / 8f), 0);
                        }
                    }
                    Fx.glowDot(ctx, A[0], A[1], p.rgb, alpha, r);
                }
                default -> { // TWINKLE
                    float tw = 0.5f + 0.5f * MathHelper.sin(now / 180f + p.phase);
                    float a = alpha * MathHelper.sin(age * 3.1416f) * (0.35f + 0.65f * tw);
                    star(ctx, A[0], A[1], p.rgb, a, p.size);
                }
            }
        }

        for (Iterator<Ring> it = RS.iterator(); it.hasNext(); ) {
            Ring g = it.next();
            float age = (now - g.born) / 1000f / g.life;
            if (age >= 1f) { it.remove(); continue; }
            float rad = g.r0 + (g.r1 - g.r0) * age;
            float alpha = 1f - age;
            boolean prev = false;
            float px = 0, py = 0;
            for (int i = 0; i <= 40; i++) {
                double th = 2.0 * Math.PI * i / 40.0;
                boolean ok = Fx.to(g.x + Math.cos(th) * rad, g.y, g.z + Math.sin(th) * rad, A);
                if (ok && prev) Fx.thinLine(ctx, px, py, A[0], A[1], g.rgb, alpha);
                px = A[0]; py = A[1]; prev = ok;
            }
        }
    }

    private static void star(DrawContext ctx, float x, float y, int rgb, float a, float size) {
        int r = Math.max(2, Math.round(size * 2f));
        Fx.thinLine(ctx, x - r, y, x + r, y, rgb, a);
        Fx.thinLine(ctx, x, y - r, x, y + r, rgb, a);
        Fx.glowDot(ctx, x, y, rgb, a, Math.max(0, Math.round(size * 0.5f)));
    }
}
