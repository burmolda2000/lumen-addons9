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
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Trails: следы за движущимися игроками (линия, лента, звёзды, точки) с градиентами. */
public final class MoveTrails {
    public static final Module M = new Module("Trails",
            "Trails behind moving players", Category.VISUALS, false, true);
    static final Setting MODE   = M.add(Setting.mode("mode", "Mode", 0, "Line", "Ribbon", "Stars", "Dots"));
    static final Setting COLOR  = M.add(Setting.mode("color", "Color", 6, Fx.PAL));
    static final Setting LENGTH = M.add(Setting.num("length", "Length (sec)", 1.2, 0.3, 3, 0.1));
    static final Setting RIBBON = M.add(Setting.num("ribbon", "Ribbon height", 0.9, 0.3, 2, 0.1));
    static final Setting SELF   = M.add(Setting.bool("self", "Show yourself", true));
    static final Setting OTHERS = M.add(Setting.bool("others", "Other players", true));

    private static final Map<Integer, LinkedList<double[]>> TRAILS = new HashMap<>();
    private static final float[] A = new float[3], B = new float[3];

    private MoveTrails() { }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity me, List<Entity> near, float td, long now) {
        if (!M.enabled) { TRAILS.clear(); return; }
        Set<Integer> seen = new HashSet<>();
        if (SELF.bool) { seen.add(me.getId()); track(ctx, me, td, now); }
        if (OTHERS.bool) {
            for (Entity e : near) {
                if (!(e instanceof PlayerEntity)) continue;
                seen.add(e.getId());
                track(ctx, e, td, now);
            }
        }
        TRAILS.keySet().retainAll(seen);
    }

    private static void track(DrawContext ctx, Entity e, float td, long now) {
        LinkedList<double[]> q = TRAILS.computeIfAbsent(e.getId(), k -> new LinkedList<>());
        Vec3d lp = e.getLerpedPos(td);
        long life = (long) (LENGTH.num * 1000.0);
        double[] last = q.isEmpty() ? null : q.getLast();
        if (last == null) {
            q.addLast(new double[] { lp.x, lp.y, lp.z, now });
        } else {
            double dx = lp.x - last[0], dy = lp.y - last[1], dz = lp.z - last[2];
            if (dx * dx + dy * dy + dz * dz >= 0.0036) q.addLast(new double[] { lp.x, lp.y, lp.z, now });
        }
        while (!q.isEmpty() && now - q.getFirst()[3] > life) q.removeFirst();
        if (q.size() < 2) return;

        List<double[]> s = new ArrayList<>(q);
        int n = s.size();
        double rib = RIBBON.num;
        for (int i = 0; i < n; i++) {
            double[] c = s.get(i);
            float age = (now - (long) c[3]) / (float) Math.max(1L, life);
            float alpha = Math.max(0f, 1f - age);
            int rgb = Fx.color(COLOR.mode, i / (float) n + now / 3000f);
            boolean ok = Fx.to(c[0], c[1] + 0.1, c[2], A);
            if (!ok) continue;

            switch (MODE.mode) {
                case 0 -> { // Line
                    if (i > 0) {
                        double[] p = s.get(i - 1);
                        if (Fx.to(p[0], p[1] + 0.1, p[2], B)) Fx.thinLine(ctx, B[0], B[1], A[0], A[1], rgb, alpha * 0.9f);
                    }
                    if (i % 4 == 0) Fx.glowDot(ctx, A[0], A[1], rgb, alpha, 0);
                }
                case 1 -> { // Ribbon
                    boolean top = Fx.to(c[0], c[1] + rib, c[2], B);
                    if (i > 0) {
                        double[] p = s.get(i - 1);
                        float ax = A[0], ay = A[1], bx = B[0], by = B[1];
                        if (Fx.to(p[0], p[1] + 0.1, p[2], A)) {
                            Fx.thinLine(ctx, A[0], A[1], ax, ay, rgb, alpha * 0.9f);
                        }
                        if (top && Fx.to(p[0], p[1] + rib, p[2], A)) {
                            Fx.thinLine(ctx, A[0], A[1], bx, by, rgb, alpha * 0.9f);
                        }
                        if (top && i % 3 == 0) Fx.thinLine(ctx, ax, ay, bx, by, rgb, alpha * 0.5f);
                    }
                }
                case 2 -> { // Stars
                    if (i % 3 == 0) {
                        Fx.thinLine(ctx, A[0] - 4, A[1], A[0] + 4, A[1], rgb, alpha);
                        Fx.thinLine(ctx, A[0], A[1] - 4, A[0], A[1] + 4, rgb, alpha);
                        Fx.glowDot(ctx, A[0], A[1], rgb, alpha, 1);
                    }
                }
                default -> Fx.glowDot(ctx, A[0], A[1], rgb, alpha, 1); // Dots
            }
        }
    }
}
