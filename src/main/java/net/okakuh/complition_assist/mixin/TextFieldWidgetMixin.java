package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.okakuh.complition_assist.Handlers;
import net.okakuh.complition_assist.Suggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Inject(method = "onChanged", at = @At("HEAD"))
    private void onChanged(String newText, CallbackInfo ci) {
        TextFieldWidget widget = (TextFieldWidget)(Object)this;
        if (widget.isFocused()) Handlers.TextFieldWidgetHandler(widget);
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void complition_assist$onHudRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        Suggestions.tryRender(context);
    }
}