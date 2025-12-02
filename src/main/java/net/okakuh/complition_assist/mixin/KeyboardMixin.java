package net.okakuh.complition_assist.mixin;

import net.okakuh.complition_assist.ComplitionAssistClient;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onChar", at = @At("HEAD"))
    private void onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        // Преобразуем код символа в char
        char character = (char) codePoint;
        ComplitionAssistClient.onCharTyped(character);
    }

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1) { // 1 = GLFW.GLFW_PRESS
            ComplitionAssistClient.onKeyPressed(key, modifiers);
        }
    }
}