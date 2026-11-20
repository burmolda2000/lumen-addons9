package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Светящийся след за оружием: золотой — обычный удар, красный — критический. */
public final class WeaponTrails {
    public static final Module M = new Module("Weapon trails",
            "Glowing trail: gold on normal hit, red on critical", Category.VISUALS, true, true);
    static final Setting LENGTH = M.add(Setting.num("length", "Length (sec)", 0.45, 0.15, 1.2, 0.05));
    static final Setting WIDTH  = M.add(Setting.num("width", "Width", 1, 0, 3, 1));
    static final Setting MISS   = M.add(Setting.bool("on_miss", "Trail on miss", true));
    static final Setting ANY    = M.add(Setting.bool("any_item", "Any item", false));

    private static final int GOLD = 0xFFD700, RED = 0xFF2A2A;

    private static final class Pt {
        final Vec3d pos; final int rgb; final long t; final boolean start;
        Pt(Vec3d pos, int rgb, long t, boolean start) { this.pos = pos; this.rgb = rgb; this.t = t; this.start = start; }
    }

    private static final List<Pt> PTS = new ArrayList<>();
    private static boolean crit;          // результат последнего удара по сущности
    private static long hitTime = -10_000;
    private static boolean wasSwinging;
    private static final long ATTACK_MS = 320;   // длительность следа от удара по сущности

    static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (M.enabled && player == MinecraftClient.getInstance().player) {
                crit = isCrit(player, entity);
                hitTime = Util.getMeasuringTimeMs();
            }
            return ActionResult.PASS;
        });
        HudElementRegistry.addLast(Identifier.of(LumenAddons.ID, "weapon_trails"),
                (ctx, tick) -> render(ctx, tick.getTickProgress(false)));
    }

    /** Условия критического удара из ванили (без fallDistance: падение определяем по скорости). */
    static boolean isCrit(PlayerEntity p, Object target) {
        return p.getAttackCooldownProgress(0.5f) > 0.9f
                && p.getVelocity().y < 0 && !p.isOnGround()
                && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS)
                && !p.hasVehicle() && !p.isSprinting()
                && target instanceof LivingEntity;
    }

    private static boolean weapon(ItemStack s) {
        if (s.isEmpty()) return false;
        if (ANY.bool) return true;
        return s.isIn(ItemTags.SWORDS) || s.isIn(ItemTags.AXES)
                || s.isOf(Items.MACE) || s.isOf(Items.TRIDENT);
    }

    private static void render(DrawContext ctx, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        long now = Util.getMeasuringTimeMs();
        long life = (long) (LENGTH.num * 1000.0);
        PTS.removeIf(pt -> now - pt.t > life);
        if (!M.enabled || p == null || mc.world == null) { PTS.clear(); return; }

        // --- добавляем новую точку, пока идёт взмах ---
        // Прогресс берём из анимации руки; если её нет (или она не успела стартовать),
        // но только что был удар по сущности — считаем прогресс по таймеру от самого удара.
        float prog = p.getHandSwingProgress(td);
        boolean recentHit = now - hitTime < 700;
        float use = -1f;
        if (prog > 0f && prog < 1f) use = prog;
        else if (now - hitTime < ATTACK_MS) use = MathHelper.clamp((now - hitTime) / (float) ATTACK_MS, 0.001f, 0.999f);
        boolean active = use > 0f;
        if (active && weapon(p.getMainHandStack())) {
            boolean recentCrit = crit && recentHit;
            if (recentHit || MISS.bool) {
                double yawOff = (0.5 - use) * 110.0;                   // справа налево
                double pitchOff = Math.sin(use * Math.PI) * 12.0 - 6.0;
                Vec3d dir = Vec3d.fromPolar((float) (p.getPitch(td) + pitchOff), (float) (p.getYaw(td) + yawOff));
                Vec3d tip = p.getCameraPosVec(td).add(0, -0.25, 0).add(dir.multiply(2.0));
                PTS.add(new Pt(tip, recentCrit ? RED : GOLD, now, !wasSwinging));
                if (PTS.size() > 240) PTS.remove(0);
            }
        }
        wasSwinging = active;
        if (PTS.size() < 2) return;

        if (!Fx.begin(mc, td, ctx)) return;
        int r = Math.max(0, (int) WIDTH.num);
        float[] a = new float[3], b = new float[3];
        boolean prevOk = false;
        for (int i = 0; i < PTS.size(); i++) {
            Pt pt = PTS.get(i);
            boolean ok = Fx.to(pt.pos.x, pt.pos.y, pt.pos.z, b);
            if (ok && prevOk && !pt.start) {
                float age = (now - pt.t) / (float) Math.max(1, life);
                float alpha = Math.max(0f, 1f - age);
                Fx.glowLine(ctx, a[0], a[1], b[0], b[1], pt.rgb, alpha, r);
            }
            float[] t = a; a = b; b = t;
            prevOk = ok;
        }
    }
}
