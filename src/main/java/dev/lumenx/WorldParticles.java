package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/** World particles: звёзды, кометы, кольца, спирали и светлячки вокруг игрока. */
public final class WorldParticles {
    public static final Module M = new Module("World particles",
            "Decorative particles around you", Category.VISUALS, false, true);
    static final Setting MODE   = M.add(Setting.mode("mode", "Mode", 0, "Stars", "Comets", "Rings", "Spiral", "Fireflies"));
    static final Setting AMOUNT = M.add(Setting.num("amount", "Amount", 40, 5, 200, 5));
    static final Setting RADIUS = M.add(Setting.num("radius", "Radius", 10, 3, 24, 1));
    static final Setting COLOR  = M.add(Setting.mode("color", "Color", 6, Fx.PAL));
    static final Setting SIZE   = M.add(Setting.num("size", "Size", 2, 1, 4, 1));

    private static long lastT;
    private static float acc;
    private static final float[] A = new float[3];

    private WorldParticles() { }

    static void draw(DrawContext ctx, ClientPlayerEntity p, float td, long now) {
        if (!M.enabled) { lastT = 0; return; }
        float dt = lastT == 0 ? 0f : MathHelper.clamp((now - lastT) / 1000f, 0f, 0.1f);
        lastT = now;
        Vec3d c = p.getLerpedPos(td);
        float t = now / 1000f;
        double r = RADIUS.num;
        float size = (float) SIZE.num;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        switch (MODE.mode) {
            case 0 -> { // Stars: мерцающие звёзды в сфере вокруг игрока
                acc += dt * (float) AMOUNT.num / 4.5f;
                while (acc >= 1f) {
                    acc -= 1f;
                    double[] d = HitFx.dir(rnd);
                    double rad = r * (0.4 + rnd.nextDouble() * 0.6);
                    Overlay.add(Overlay.TWINKLE, c.x + d[0] * rad, c.y + 1.0 + d[1] * rad, c.z + d[2] * rad,
                            (rnd.nextDouble() - 0.5) * 0.2, (rnd.nextDouble() - 0.5) * 0.2, (rnd.nextDouble() - 0.5) * 0.2,
                            0f, 3f + rnd.nextFloat() * 3f, size, Fx.color(COLOR.mode, rnd.nextFloat()));
                }
            }
            case 1 -> { // Comets: кометы с хвостами пролетают сверху
                acc += dt * (float) AMOUNT.num / 60f;
                while (acc >= 1f) {
                    acc -= 1f;
                    double a = rnd.nextDouble() * Math.PI * 2.0;
                    Overlay.add(Overlay.COMET, c.x + Math.cos(a) * r, c.y + 4.0 + rnd.nextDouble() * 5.0, c.z + Math.sin(a) * r,
                            -Math.sin(a) * 6.0 + (rnd.nextDouble() - 0.5), -2.5, Math.cos(a) * 6.0 + (rnd.nextDouble() - 0.5),
                            0f, 1.4f, size, Fx.color(COLOR.mode, rnd.nextFloat()));
                }
            }
            case 2 -> { // Rings: три вращающихся кольца вокруг игрока (без состояния)
                for (int k = 0; k < 3; k++) {
                    double y = c.y + 0.3 + k * 0.8;
                    double rad = Math.min(r, 6.0) * 0.25 * (1.0 + k * 0.25);
                    int n = 28;
                    for (int i = 0; i < n; i++) {
                        double ang = i * 2.0 * Math.PI / n + t * (0.8 + 0.4 * k) * (k % 2 == 0 ? 1 : -1);
                        if (Fx.to(c.x + Math.cos(ang) * rad, y, c.z + Math.sin(ang) * rad, A)) {
                            Fx.glowDot(ctx, A[0], A[1], Fx.color(COLOR.mode, i / (float) n + t * 0.2f), 0.9f, Math.max(0, (int) size - 1));
                        }
                    }
                }
            }
            case 3 -> { // Spiral: спираль поднимается вокруг игрока
                int n = 44;
                double rad = Math.min(r, 6.0) * 0.18;
                for (int i = 0; i < n; i++) {
                    float f = i / (float) n;
                    double y = c.y + ((f + t * 0.25f) % 1.0f) * 3.0;
                    double ang = f * Math.PI * 6.0 + t * 2.0;
                    if (Fx.to(c.x + Math.cos(ang) * (rad + 0.3), y, c.z + Math.sin(ang) * (rad + 0.3), A)) {
                        Fx.glowDot(ctx, A[0], A[1], Fx.color(COLOR.mode, f + t * 0.2f), 0.85f, Math.max(0, (int) size - 1));
                    }
                }
            }
            default -> { // Fireflies: светлячки у земли
                acc += dt * (float) AMOUNT.num / 6f;
                while (acc >= 1f) {
                    acc -= 1f;
                    double a = rnd.nextDouble() * Math.PI * 2.0;
                    double rad = r * Math.sqrt(rnd.nextDouble());
                    Overlay.add(Overlay.TWINKLE, c.x + Math.cos(a) * rad, c.y + 0.2 + rnd.nextDouble() * 2.0, c.z + Math.sin(a) * rad,
                            (rnd.nextDouble() - 0.5) * 0.5, (rnd.nextDouble() - 0.5) * 0.3, (rnd.nextDouble() - 0.5) * 0.5,
                            0f, 5f + rnd.nextFloat() * 3f, size, Fx.color(COLOR.mode == 6 ? 4 : COLOR.mode, rnd.nextFloat()));
                }
            }
        }
    }
}
