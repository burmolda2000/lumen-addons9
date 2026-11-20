package dev.lumenx.mixin;

import dev.lumenx.CustomHand;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** priority 900: срабатываем раньше, чем миксин модуля Swing из Lumen. */
@Mixin(value = HeldItemRenderer.class, priority = 900)
public abstract class HeldItemRendererMixin {
    @Inject(method = "applyEquipOffset", at = @At("TAIL"))
    private void lumenx$offset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        CustomHand.applyOffset(matrices, arm, equipProgress);
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void lumenx$swing(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        if (CustomHand.applySwing(matrices, arm, swingProgress)) ci.cancel();
    }
}
