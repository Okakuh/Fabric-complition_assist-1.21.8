package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.okakuh.complition_assist.ComplitionAssist;
import net.okakuh.complition_assist.Handlers;
import net.okakuh.complition_assist.Suggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Inject(method = "onChanged", at = @At("RETURN"))
    private void complition_assist$onChanged(String newText, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        Handlers.TextFieldWidgetHandler((TextFieldWidget)(Object)this);
    }

    @Inject(method = "setFocused", at = @At("RETURN"))
    private void complition_assist$setFocused(boolean focused, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        if (focused) {
            Handlers.TextFieldWidgetHandler((TextFieldWidget)(Object)this);
        } else {
            Suggestions.OFF();
        }
    }
}