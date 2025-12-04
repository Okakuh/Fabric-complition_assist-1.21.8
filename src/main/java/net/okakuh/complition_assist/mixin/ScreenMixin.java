package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.okakuh.complition_assist.ComplitionAssist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    // Отслеживаем открытие экрана
    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("HEAD"))
    private void onScreenInit(CallbackInfo ci) {
        ComplitionAssist.screenChanged();
    }

    // Отслеживаем закрытие экрана (когда экран закрывается)
    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        ComplitionAssist.screenChanged();
    }

    // Также можно отследить, когда экран становится невидимым
    @Inject(method = "removed", at = @At("HEAD"))
    private void onScreenRemoved(CallbackInfo ci) {
        ComplitionAssist.screenChanged();
    }
}