package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.okakuh.complition_assist.ComplitionAssist;
import net.okakuh.complition_assist.suggesters.TextFieldWidgetSuggester;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Unique
    private boolean initialized = false;

    @Inject(method = "setFocused", at = @At("RETURN"))
    private void complition_assist$setFocused(boolean focused, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        if (initialized) return;
        initialized = true;

        TextFieldWidget widget = (TextFieldWidget)(Object)this;
        new TextFieldWidgetSuggester(widget);
    }
}