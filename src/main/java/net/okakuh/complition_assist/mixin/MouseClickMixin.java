package net.okakuh.complition_assist.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.Window;
import net.okakuh.complition_assist.ComplitionAssist;
import net.okakuh.complition_assist.Suggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseClickMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        if (!Suggestions.isON()) return;

        if (button == 0 && action == 1) { // Левая кнопка нажата
            Mouse mouse = (Mouse)(Object)this;
            MinecraftClient client = MinecraftClient.getInstance();
            Window wind = client.getWindow();
            Suggestions.onMouseClick(mouse.getScaledX(wind), mouse.getScaledY(wind));
        }
    }
}