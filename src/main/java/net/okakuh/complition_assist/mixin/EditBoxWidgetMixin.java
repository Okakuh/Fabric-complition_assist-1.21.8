package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.okakuh.complition_assist.Handlers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBoxWidget.class)
public abstract class EditBoxWidgetMixin {
    @Shadow
    private EditBox editBox;
    @Inject(method = "setText(Ljava/lang/String;Z)V", at = @At("TAIL"))
    private void onTextChanged(String text, boolean allowOverflow, CallbackInfo ci) {
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (widget.isFocused() && this.editBox != null) {
            Handlers.EditBoxWidgetHandler(widget, this.editBox);
        }
    }

    @Inject(method = "onCursorChange()V", at = @At("TAIL"))
    private void onCursorChange(CallbackInfo ci) {
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (widget.isFocused() && this.editBox != null) {
            Handlers.EditBoxWidgetHandler(widget, this.editBox);
        }
    }

    // Ловим изменение фокуса
    @Inject(method = "setFocused", at = @At("TAIL"))
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (focused && this.editBox != null) {
            Handlers.EditBoxWidgetHandler(widget, this.editBox);
        }
    }
}