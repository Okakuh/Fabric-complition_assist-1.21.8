package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.okakuh.complition_assist.ComplitionAssistClient;
import net.okakuh.complition_assist.InputFieldTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Обновляем трекер
        InputFieldTracker.update();

        // Если есть активное поле и предложения - рисуем
        if (ComplitionAssistClient.isTracking() &&
                !ComplitionAssistClient.getCurrentSuggestions().isEmpty() &&
                InputFieldTracker.hasActiveField()) {

            ComplitionAssistClient.renderSuggestionsHud(context);
        }
    }
}