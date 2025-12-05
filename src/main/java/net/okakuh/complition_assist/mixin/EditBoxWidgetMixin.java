package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.okakuh.complition_assist.ComplitionAssist;
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
            ComplitionAssist.trigerEditBoxWidget(
                    widget,
                    this.editBox
            );
        }
    }

    @Inject(method = "onCursorChange()V", at = @At("TAIL"))
    private void onCursorChange(CallbackInfo ci) {
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (widget.isFocused() && this.editBox != null) {
            ComplitionAssist.trigerEditBoxWidget(
                    widget,
                    this.editBox
            );
        }
    }

    // Ловим изменение фокуса
    @Inject(method = "setFocused", at = @At("TAIL"))
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (focused && this.editBox != null) {
            ComplitionAssist.trigerEditBoxWidget(
                    widget,
                    this.editBox
            );
        }
    }

    @Inject(method = "renderOverlay", at = @At("RETURN"))
    private void onRenderOverlay(DrawContext context, CallbackInfo ci) {
        ComplitionAssist.render(context, false);
    }
}