package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.okakuh.complition_assist.ComplitionAssist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Inject(method = "onChanged", at = @At("HEAD"))
    private void onChanged(String newText, CallbackInfo ci) {
        TextFieldWidget widget = (TextFieldWidget)(Object)this;
        if (widget.isFocused()) ComplitionAssist.trigerTextFieldWidget(widget);
    }
}