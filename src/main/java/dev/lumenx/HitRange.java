package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Hit range: круг радиуса атаки под игроками. Зелёный — цель в радиусе, красный — цели нет. */
public final class HitRange {
    public static final Module M = new Module("Hit range",
            "Attack radius circle under players: green = target in range, red = none",
            Category.VISUALS, true, true);
    static final Setting RADIUS  = M.add(Setting.num("radius", "Radius", 3, 1, 6, 0.5));
    static final Setting SELF    = M.add(Setting.bool("self", "Show for yourself", true));
    static final Setting OTHERS  = M.add(Setting.bool("others", "Show for other players", true));
    static final Setting TARGETS = M.add(Setting.mode("targets", "Targets", 0, "Players", "All living"));
    static final Setting MAXDIST = M.add(Setting.num("max_dist", "Max distance", 24, 8, 64, 4));

    private static final int GREEN = 0x55FF55, RED = 0xFF4444, SEGMENTS = 48;

    private HitRange() { }

    static void init() {
        HudElementRegistry.addLast(Identifier.of(LumenAddons.ID, "hit_range"),
                (ctx, tick) -> render(ctx, tick.getTickProgress(false)));
    }

    private static boolean isTarget(Entity e) {
        if (!e.isAlive() || e.isSpectator()) return false;
        if (TARGETS.mode == 0) return e instanceof PlayerEntity;
        return e instanceof LivingEntity && !(e instanceof ArmorStandEntity);
    }

    /** Расстояние от точки до ближайшей точки коробки (0, если внутри). */
    private static double distToBox(Vec3d p, Box b) {
        double dx = Math.max(Math.max(b.minX - p.x, 0), p.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - p.y, 0), p.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - p.z, 0), p.z - b.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void render(DrawContext ctx, float td) {
        if (!M.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity me = mc.player;
        ClientWorld w = mc.world;
        if (me == null || w == null || mc.currentScreen != null) return;
        if (!Fx.begin(mc, td, ctx)) return;

        double r = RADIUS.num;
        double maxD2 = MAXDIST.num * MAXDIST.num;

        for (AbstractClientPlayerEntity p : w.getPlayers()) {
            boolean self = p == me;
            if (self ? !SELF.bool : !OTHERS.bool) continue;
            if (p.isSpectator() || !p.isAlive()) continue;
            if (!self && me.squaredDistanceTo(p) > maxD2) continue;

            // есть ли цель в радиусе атаки (от глаз игрока до ближайшей точки хитбокса цели)
            Vec3d eye = p.getCameraPosVec(td);
            boolean has = false;
            Box area = p.getBoundingBox().expand(r + 1.0);
            for (Entity e : w.getOtherEntities(p, area, HitRange::isTarget)) {
                if (distToBox(eye, e.getBoundingBox()) <= r) { has = true; break; }
            }
            int rgb = has ? GREEN : RED;

            Vec3d c = p.getLerpedPos(td);
            double y = c.y + 0.02;
            float[] a = new float[3], b = new float[3];
            boolean prevOk = false;
            for (int i = 0; i <= SEGMENTS; i++) {
                double th = 2.0 * Math.PI * i / SEGMENTS;
                boolean ok = Fx.to(c.x + Math.cos(th) * r, y, c.z + Math.sin(th) * r, b);
                if (ok && prevOk) Fx.thinLine(ctx, a[0], a[1], b[0], b[1], rgb, 0.9f);
                float[] t = a; a = b; b = t;
                prevOk = ok;
            }
        }
    }
}
