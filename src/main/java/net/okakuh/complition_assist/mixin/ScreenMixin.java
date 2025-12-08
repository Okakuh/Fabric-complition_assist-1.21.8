package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.okakuh.complition_assist.ComplitionAssist;
import net.okakuh.complition_assist.Suggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("HEAD"))
    private void complition_assist$onScreenInit(CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        Suggestions.OFF();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void complition_assist$onScreenClose(CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        Suggestions.OFF();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void complition_assist$onScreenRemoved(CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        Suggestions.OFF();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void complition_assist$onRenderOverlay(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        if (Suggestions.isUsingScreenDrawContext()) {
            Suggestions.tryRender(context);
        }
    }
}