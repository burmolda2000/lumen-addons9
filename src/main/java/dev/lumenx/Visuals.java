package dev.lumenx;

import dev.lumen.util.Proj;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.List;

/** Один HUD-элемент, который по очереди рисует все визуальные модули аддона. */
final class Visuals {
    private Visuals() { }

    static void init() {
        HudElementRegistry.addLast(Identifier.of(LumenAddons.ID, "visuals"),
                (ctx, tick) -> render(ctx, tick.getTickProgress(false)));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (player == mc.player && entity instanceof LivingEntity le) {
                boolean crit = WeaponTrails.isCrit(player, entity);
                long now = Util.getMeasuringTimeMs();
                HitColor.onHit(le, crit, now);
                HitFx.onHit(le, crit);
                KillEffect.onHit(le, now);
            }
            return ActionResult.PASS;
        });
    }

    private static void render(DrawContext ctx, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;
        long now = Util.getMeasuringTimeMs();

        // логика без проекции
        KillEffect.update(now);
        TotemTracker.update(w, p, now);
        MaceHelper.update(p);
        AspectRatio.tick(p, now);

        if (mc.currentScreen == null
                && Fx.begin(mc, td, ctx)) {
            boolean need = HitColor.M.enabled || Hitbox.M.enabled || BodyGlow.M.enabled
                    || ChinaHat.M.enabled || MoveTrails.M.enabled;
            List<Entity> near = need
                    ? w.getOtherEntities(p, p.getBoundingBox().expand(64.0),
                            e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())
                    : List.<Entity>of();

            HitColor.draw(ctx, now, td);
            Hitbox.draw(ctx, mc, p, near, td, now);
            BodyGlow.draw(ctx, mc, p, near, td, now);
            ChinaHat.draw(ctx, mc, p, near, td, now);
            MoveTrails.draw(ctx, mc, p, near, td, now);
            BlockOverlay.draw(ctx, mc, td, now);
            ArrowNametag.draw(ctx, mc, w, p, td);
            WorldParticles.draw(ctx, p, td, now);
            Overlay.render(ctx, now);
        }

        MaceHelper.hud(ctx, mc, p);
        TotemTracker.hud(ctx, mc, now);
    }
}
