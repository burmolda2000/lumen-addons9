package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Предсказание траектории: жемчуг, снежок, яйцо, зелья, опыт, трезубец, лук, арбалет. */
public final class Trajectory {
    public static final Module M = new Module("Trajectory",
            "Predicts where thrown items and arrows will land", Category.VISUALS, true, true);
    static final Setting LINE   = M.add(Setting.bool("line", "Path line", true));
    static final Setting MARKER = M.add(Setting.bool("marker", "Landing marker", true));
    static final Setting ENT    = M.add(Setting.bool("entity", "Highlight entity hit", true));
    static final Setting TICKS  = M.add(Setting.num("ticks", "Max ticks", 120, 20, 300, 10));
    static final Setting THROW  = M.add(Setting.bool("throwables", "Throwables (pearl, snowball...)", true));
    static final Setting BOW    = M.add(Setting.bool("bow", "Bow and crossbow", true));
    static final Setting TRI    = M.add(Setting.bool("trident", "Trident", true));
    static final Setting POTION = M.add(Setting.bool("potions", "Potions and XP bottles", true));

    private record Shot(double speed, double gravity, double drag, double waterDrag, float pitchOffset) { }
    private record Sim(List<Vec3d> pts, Vec3d end, boolean entity) { }

    static void init() {
        HudElementRegistry.addLast(Identifier.of(LumenAddons.ID, "trajectory"),
                (ctx, tick) -> render(ctx, tick.getTickProgress(false)));
    }

    // ---------- что сейчас в руках ----------
    private static Shot shotFor(ClientPlayerEntity p) {
        for (ItemStack s : new ItemStack[] { p.getMainHandStack(), p.getOffHandStack() }) {
            if (s.isEmpty()) continue;
            Item i = s.getItem();
            if (THROW.bool && (i == Items.ENDER_PEARL || i == Items.SNOWBALL || i == Items.EGG))
                return new Shot(1.5, 0.03, 0.99, 0.8, 0f);
            if (POTION.bool && i == Items.EXPERIENCE_BOTTLE)
                return new Shot(0.7, 0.07, 0.99, 0.8, -20f);
            if (POTION.bool && (i == Items.SPLASH_POTION || i == Items.LINGERING_POTION))
                return new Shot(0.5, 0.05, 0.99, 0.8, -20f);
            if (TRI.bool && i == Items.TRIDENT)
                return new Shot(2.5, 0.05, 0.99, 0.99, 0f);
            if (BOW.bool && i == Items.BOW && p.isUsingItem() && p.getActiveItem() == s) {
                float f = p.getItemUseTime() / 20f;
                f = (f * f + f * 2f) / 3f;
                if (f < 0.1f) continue;
                if (f > 1f) f = 1f;
                return new Shot(f * 3.0, 0.05, 0.99, 0.6, 0f);
            }
            if (BOW.bool && i == Items.CROSSBOW) {
                ChargedProjectilesComponent c = s.get(DataComponentTypes.CHARGED_PROJECTILES);
                if (c != null && !c.isEmpty()) {
                    boolean rocket = c.contains(Items.FIREWORK_ROCKET);
                    return rocket ? new Shot(1.6, 0.0, 0.99, 0.99, 0f)
                                  : new Shot(3.15, 0.05, 0.99, 0.6, 0f);
                }
            }
        }
        return null;
    }

    // ---------- симуляция полёта ----------
    private static Sim simulate(MinecraftClient mc, ClientPlayerEntity p, Shot s, float td) {
        ClientWorld w = mc.world;
        Vec3d pos = p.getCameraPosVec(td).subtract(0, 0.1, 0);
        double rad = Math.PI / 180.0;
        float yaw = p.getYaw(td), pitch = p.getPitch(td);
        double vx = -Math.sin(yaw * rad) * Math.cos(pitch * rad);
        double vy = -Math.sin((pitch + s.pitchOffset()) * rad);
        double vz = Math.cos(yaw * rad) * Math.cos(pitch * rad);
        Vec3d v = new Vec3d(vx, vy, vz).normalize().multiply(s.speed());
        Vec3d pv = p.getVelocity();
        v = v.add(pv.x, p.isOnGround() ? 0 : pv.y, pv.z);

        List<Vec3d> pts = new ArrayList<>();
        pts.add(pos);
        int max = Math.max(20, (int) TICKS.num);
        for (int i = 0; i < max; i++) {
            Vec3d next = pos.add(v);
            BlockHitResult bh = w.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            boolean blockHit = bh.getType() != HitResult.Type.MISS;
            Vec3d limit = blockHit ? bh.getPos() : next;

            if (ENT.bool) {
                Vec3d eHit = null;
                double best = Double.MAX_VALUE;
                Box sweep = new Box(pos, limit).expand(1.0);
                for (Entity e : w.getOtherEntities(p, sweep, Trajectory::hittable)) {
                    Optional<Vec3d> h = e.getBoundingBox().expand(0.3).raycast(pos, limit);
                    if (h.isPresent()) {
                        double d = pos.squaredDistanceTo(h.get());
                        if (d < best) { best = d; eHit = h.get(); }
                    }
                }
                if (eHit != null) {
                    pts.add(eHit);
                    return new Sim(pts, eHit, true);
                }
            }
            if (blockHit) {
                pts.add(bh.getPos());
                return new Sim(pts, bh.getPos(), false);
            }
            pos = next;
            pts.add(pos);
            boolean water = w.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER);
            v = v.multiply(water ? s.waterDrag() : s.drag()).subtract(0, s.gravity(), 0);
            if (pos.y < w.getBottomY() - 16) break;
        }
        return new Sim(pts, null, false);
    }

    private static boolean hittable(Entity e) {
        return e.isAlive() && !e.isSpectator()
                && !(e instanceof ItemEntity) && !(e instanceof ProjectileEntity);
    }

    // ---------- отрисовка (HUD-оверлей через Proj, как в остальном Lumen) ----------
    private static void render(DrawContext ctx, float td) {
        if (!M.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) return;

        Shot shot = shotFor(p);
        if (shot == null) return;
        Sim sim = simulate(mc, p, shot, td);

        if (!Fx.begin(mc, td, ctx)) return;

        int lineRgb = sim.entity() ? 0xFF4040 : 0x66E0FF;
        float[] a = new float[3], b = new float[3];
        if (LINE.bool) {
            boolean prevOk = false;
            int n = sim.pts().size();
            for (int i = 0; i < n; i++) {
                Vec3d q = sim.pts().get(i);
                boolean ok = Fx.to(q.x, q.y, q.z, b);
                if (ok && prevOk) {
                    float fade = 1f - 0.55f * (i / (float) Math.max(1, n));
                    Fx.glowLine(ctx, a[0], a[1], b[0], b[1], lineRgb, 0.75f * fade, 0);
                }
                float[] t = a; a = b; b = t;
                prevOk = ok;
            }
        }
        if (MARKER.bool && sim.end() != null) {
            Vec3d e = sim.end();
            float[] o = new float[3];
            if (Fx.to(e.x, e.y, e.z, o)) {
                int rgb = sim.entity() ? 0xFF3030 : 0xFFFFFF;
                Fx.glowDot(ctx, o[0], o[1], rgb, 0.9f, 2);
                for (int k = 0; k < 8; k++) {
                    double ang = k * Math.PI / 4.0;
                    Fx.glowDot(ctx, (float) (o[0] + Math.cos(ang) * 6), (float) (o[1] + Math.sin(ang) * 6),
                            rgb, 0.6f, 0);
                }
            }
        }
    }
}
