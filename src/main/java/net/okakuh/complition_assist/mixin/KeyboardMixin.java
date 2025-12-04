package net.okakuh.complition_assist.mixin;

import net.okakuh.complition_assist.ComplitionAssist;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1 || action == 2) { // И первое нажатие, и повтор при удержании
            ComplitionAssist.onKeyPresed(key, modifiers);
        }
    }
}