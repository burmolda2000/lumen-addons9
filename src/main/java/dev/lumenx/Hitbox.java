package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Hitbox customizer: свои контуры, углы и заливка хитбоксов, линия глаз и взгляда. */
public final class Hitbox {
    public static final Module M = new Module("Hitbox customizer",
            "Custom hitbox outlines, corners and fill", Category.VISUALS, false, true);
    static final Setting TARGETS      = M.add(Setting.mode("targets", "Targets", 0, "Players", "All living"));
    static final Setting STYLE        = M.add(Setting.mode("style", "Style", 0, "Outline", "Corners", "Filled", "Outline+Fill"));
    static final Setting COLOR        = M.add(Setting.mode("color", "Color", 0, Fx.PAL));
    static final Setting TARGET_COLOR = M.add(Setting.mode("target_color", "Target color", 2, Fx.PAL));
    static final Setting HEALTH       = M.add(Setting.bool("health", "Color by health", false));
    static final Setting EYE          = M.add(Setting.bool("eye", "Eye line", true));
    static final Setting LOOK         = M.add(Setting.bool("look", "Look line", false));
    static final Setting SELF         = M.add(Setting.bool("self", "Show yourself", false));
    static final Setting FILL         = M.add(Setting.num("fill", "Fill strength (%)", 15, 5, 60, 5));
    static final Setting MAXD         = M.add(Setting.num("max_dist", "Max distance", 48, 8, 128, 8));

    private static final float[] A = new float[3], B = new float[3];

    private Hitbox() { }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity me, List<Entity> near, float td, long now) {
        if (!M.enabled) return;
        Entity aimed = mc.crosshairTarget instanceof EntityHitResult r ? r.getEntity() : null;
        double md2 = MAXD.num * MAXD.num;
        if (SELF.bool && !Fx.firstPerson(mc)) one(ctx, me, aimed, td, now);
        for (Entity e : near) {
            if (TARGETS.mode == 0 && !(e instanceof PlayerEntity)) continue;
            if (e instanceof ArmorStandEntity) continue;
            if (me.squaredDistanceTo(e) > md2) continue;
            one(ctx, e, aimed, td, now);
        }
    }

    private static void one(DrawContext ctx, Entity e, Entity aimed, float td, long now) {
        float[][] pts = Fx.PTS;
        Box b = Fx.lerped(e, td);
        if (!Fx.projectBox(b, pts)) return;

        int rgb;
        if (e == aimed) rgb = Fx.color(TARGET_COLOR.mode, now / 1500f);
        else if (HEALTH.bool && e instanceof LivingEntity le) {
            rgb = Fx.lerp(0xFF3B3B, 0x55FF7A, MathHelper.clamp(le.getHealth() / le.getMaxHealth(), 0f, 1f));
        } else rgb = Fx.color(COLOR.mode, now / 1500f);

        int st = STYLE.mode;
        if (st >= 2) {
            Fx.rect(pts, Fx.RECT);
            Fx.fill(ctx, (int) Fx.RECT[0], (int) Fx.RECT[1], (int) Fx.RECT[2], (int) Fx.RECT[3],
                    rgb, (float) FILL.num / 100f);
        }
        if (st == 0 || st == 3) Fx.boxEdges(ctx, pts, rgb, 0.9f);
        if (st == 1) Fx.boxCorners(ctx, pts, rgb, 0.95f, 0.28f);

        double eyeY = b.minY + (b.maxY - b.minY) * 0.9;
        if (EYE.bool) {
            double[][] c = { { b.minX, b.minZ }, { b.maxX, b.minZ }, { b.maxX, b.maxZ }, { b.minX, b.maxZ } };
            boolean prevOk = false, firstOk = false;
            float fx = 0, fy = 0, px = 0, py = 0;
            for (int i = 0; i <= 4; i++) {
                double[] q = c[i % 4];
                boolean ok = Fx.to(q[0], eyeY, q[1], A);
                if (i == 0) { firstOk = ok; fx = A[0]; fy = A[1]; }
                if (ok && prevOk) Fx.thinLine(ctx, px, py, A[0], A[1], rgb, 0.8f);
                px = A[0]; py = A[1]; prevOk = ok;
            }
        }
        if (LOOK.bool) {
            double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
            Vec3d d = Vec3d.fromPolar(e.getPitch(), e.getYaw()).multiply(2.0);
            if (Fx.to(cx, eyeY, cz, A) && Fx.to(cx + d.x, eyeY + d.y, cz + d.z, B)) {
                Fx.thinLine(ctx, A[0], A[1], B[0], B[1], rgb, 0.9f);
            }
        }
    }
}
