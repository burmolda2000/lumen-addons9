package dev.lumenx;

import dev.lumen.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** 2D-примитивы для HUD-оверлея, палитры и проекция боксов. */
final class Fx {
    private Fx() { }

    // ---------- палитры ----------
    static final String[] PAL = { "Violet", "Gold", "Red", "Cyan", "Green", "White", "Rainbow", "Fire", "Ocean", "Candy" };
    private static final int[] SOLID = { 0xB07CE0, 0xFFD700, 0xFF3B3B, 0x55E0FF, 0x55FF7A, 0xFFFFFF };
    private static final int[][] STOPS = {
            { 0xFFE066, 0xFF8A00, 0xFF2A2A },   // Fire
            { 0x55E0FF, 0x3B6BFF, 0xB07CE0 },   // Ocean
            { 0xFF7AD9, 0xB07CE0, 0x55E0FF }    // Candy
    };

    static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        switch ((int) (h * 6f) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255f) << 16) | (Math.round((g + m) * 255f) << 8) | Math.round((b + m) * 255f);
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Цвет по индексу палитры; phase 0..1 двигает градиенты/радугу. */
    static int color(int idx, float phase) {
        idx = Math.max(0, Math.min(PAL.length - 1, idx));
        if (idx < 6) return SOLID[idx];
        float t = phase - (float) Math.floor(phase);
        if (idx == 6) return hsv(t, 0.75f, 1f);
        int[] st = STOPS[Math.min(idx - 7, STOPS.length - 1)];
        float tri = t < 0.5f ? t * 2f : (1f - t) * 2f;
        float f = tri * (st.length - 1);
        int i = Math.min(st.length - 2, (int) f);
        return lerp(st[i], st[i + 1], f - i);
    }

    // ---------- базовые рисовалки ----------
    static void fill(DrawContext c, int x1, int y1, int x2, int y2, int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        if (a == 0) return;
        c.fill(x1, y1, x2, y2, (a << 24) | (rgb & 0xFFFFFF));
    }

    /** Точка с мягким свечением (три слоя с разной прозрачностью). */
    static void glowDot(DrawContext c, float x, float y, int rgb, float alpha, int r) {
        int ix = Math.round(x), iy = Math.round(y);
        fill(c, ix - r - 2, iy - r - 2, ix + r + 3, iy + r + 3, rgb, alpha * 0.10f);
        fill(c, ix - r - 1, iy - r - 1, ix + r + 2, iy + r + 2, rgb, alpha * 0.22f);
        fill(c, ix - r, iy - r, ix + r + 1, iy + r + 1, rgb, alpha * 0.90f);
    }

    /** Светящаяся линия из точек с шагом ~2 px. */
    static void glowLine(DrawContext c, float x1, float y1, float x2, float y2,
                         int rgb, float alpha, int r) {
        float dx = x2 - x1, dy = y2 - y1;
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 2f));
        if (n > 200) return;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            glowDot(c, x1 + dx * t, y1 + dy * t, rgb, alpha, r);
        }
    }

    /** Тонкая линия из точек 2x2 без свечения (дёшево, для кругов и разметки). */
    static void thinLine(DrawContext c, float x1, float y1, float x2, float y2, int rgb, float alpha) {
        float dx = x2 - x1, dy = y2 - y1;
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 1.5f));
        if (n > 300) return;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            int x = Math.round(x1 + dx * t), y = Math.round(y1 + dy * t);
            fill(c, x, y, x + 2, y + 2, rgb, alpha);
        }
    }

    /** Свечение вокруг прямоугольника. */
    static void rectGlow(DrawContext c, int x1, int y1, int x2, int y2, int rgb, float a) {
        for (int i = 0; i < 3; i++) {
            int d = i + 1;
            float al = a * (i == 0 ? 0.8f : i == 1 ? 0.35f : 0.15f);
            fill(c, x1 - d, y1 - d, x2 + d, y1 - d + 1, rgb, al);
            fill(c, x1 - d, y2 + d - 1, x2 + d, y2 + d, rgb, al);
            fill(c, x1 - d, y1 - d, x1 - d + 1, y2 + d, rgb, al);
            fill(c, x2 + d - 1, y1 - d, x2 + d, y2 + d, rgb, al);
        }
    }

    /** Заливка выпуклого четырёхугольника горизонтальными полосами. */
    static void fillQuad(DrawContext c, float[] xs, float[] ys, int rgb, float alpha) {
        float minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
        float maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
        int y0 = (int) Math.floor(minY), y1 = (int) Math.ceil(maxY);
        if (y1 - y0 > 1500) return;
        for (int y = y0; y < y1; y += 2) {
            float yy = y + 1f, lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                float ya = ys[i], yb = ys[j];
                if ((ya <= yy && yb > yy) || (yb <= yy && ya > yy)) {
                    float x = xs[i] + (yy - ya) * (xs[j] - xs[i]) / (yb - ya);
                    lo = Math.min(lo, x);
                    hi = Math.max(hi, x);
                }
            }
            if (hi > lo) fill(c, Math.round(lo), y, Math.round(hi), y + 2, rgb, alpha);
        }
    }

    // ---------- 3D-боксы ----------
    static final float[][] PTS = new float[8][3];
    static final float[] RECT = new float[4];
    static final int[][] EDGES = {
            { 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 }, { 0, 2 }, { 1, 3 }, { 4, 6 }, { 5, 7 }, { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 }
    };

    /** Коэффициент растяжения экрана (Aspect ratio); 1 — без растяжения. */
    static float stretch = 1f;

    /** Готовит проекцию Lumen с учётом соотношения сторон. */
    static boolean begin(MinecraftClient mc, float td, DrawContext ctx) {
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float f = AspectRatio.factor(w, h);
        int w2 = Math.max(1, Math.round(w * f));
        stretch = w2 / (float) w;
        return Proj.begin(mc, td, w2, h);
    }

    /** Проекция точки мира на экран; x возвращается в реальных пикселях экрана. */
    static boolean to(double x, double y, double z, float[] out) {
        if (!Proj.to(x, y, z, out)) return false;
        if (stretch != 1f) out[0] /= stretch;
        return true;
    }

    static boolean firstPerson(MinecraftClient mc) {
        return mc.options.getPerspective().isFirstPerson();
    }

    /** Хитбокс сущности, сдвинутый в интерполированную позицию. */
    static Box lerped(Entity e, float td) {
        Box b = e.getBoundingBox();
        Vec3d lp = e.getLerpedPos(td);
        double dx = lp.x - (b.minX + b.maxX) / 2.0;
        double dy = lp.y - b.minY;
        double dz = lp.z - (b.minZ + b.maxZ) / 2.0;
        return b.offset(dx, dy, dz);
    }

    /** Проецирует 8 углов; false, если хоть один за камерой. */
    static boolean projectBox(Box b, float[][] out) {
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) != 0 ? b.maxX : b.minX;
            double y = (i & 2) != 0 ? b.maxY : b.minY;
            double z = (i & 4) != 0 ? b.maxZ : b.minZ;
            if (!to(x, y, z, out[i])) return false;
        }
        return true;
    }

    static void rect(float[][] p, float[] out) {
        float x1 = Float.MAX_VALUE, y1 = Float.MAX_VALUE, x2 = -Float.MAX_VALUE, y2 = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            x1 = Math.min(x1, p[i][0]); x2 = Math.max(x2, p[i][0]);
            y1 = Math.min(y1, p[i][1]); y2 = Math.max(y2, p[i][1]);
        }
        out[0] = x1; out[1] = y1; out[2] = x2; out[3] = y2;
    }

    static void boxEdges(DrawContext c, float[][] p, int rgb, float a) {
        for (int[] e : EDGES) thinLine(c, p[e[0]][0], p[e[0]][1], p[e[1]][0], p[e[1]][1], rgb, a);
    }

    static void boxCorners(DrawContext c, float[][] p, int rgb, float a, float frac) {
        for (int[] e : EDGES) {
            float x1 = p[e[0]][0], y1 = p[e[0]][1], x2 = p[e[1]][0], y2 = p[e[1]][1];
            thinLine(c, x1, y1, x1 + (x2 - x1) * frac, y1 + (y2 - y1) * frac, rgb, a);
            thinLine(c, x2, y2, x2 + (x1 - x2) * frac, y2 + (y1 - y2) * frac, rgb, a);
        }
    }
}
