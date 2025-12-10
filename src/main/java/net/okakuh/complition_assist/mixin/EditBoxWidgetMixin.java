package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.okakuh.complition_assist.ComplitionAssist;
import net.okakuh.complition_assist.suggesters.EditBoxSuggester;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBoxWidget.class)
public abstract class EditBoxWidgetMixin {
    @Unique
    private boolean initialized = false;
    @Unique
    private EditBoxSuggester d;

    @Shadow
    private EditBox editBox;

    @Inject(method = "setText(Ljava/lang/String;Z)V", at = @At("TAIL"))
    private void onTextChanged(String text, boolean allowOverflow, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (widget.isFocused() && this.editBox != null) {
            if (!initialized) {
                initialized = true;
                d = new EditBoxSuggester(widget, this.editBox);
            }
            d.onChanged();
        }
    }

    @Inject(method = "onCursorChange()V", at = @At("TAIL"))
    private void onCursorChange(CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (widget.isFocused() && this.editBox != null) {
            if (!initialized) {
                initialized = true;
                d = new EditBoxSuggester(widget, this.editBox);
            }
            d.onChanged();
        }
    }

    // Ловим изменение фокуса
    @Inject(method = "setFocused", at = @At("TAIL"))
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        EditBoxWidget widget = (EditBoxWidget)(Object)this;

        if (focused && this.editBox != null) {
            if (!initialized) {
                initialized = true;
                d = new EditBoxSuggester(widget, this.editBox);
            }
            d.onChanged();
        }
    }
}