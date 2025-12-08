package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.okakuh.complition_assist.ComplitionAssist;
import net.okakuh.complition_assist.Handlers;
import net.okakuh.complition_assist.Suggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void complition_assist$onInit(CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        AbstractSignEditScreen screen = (AbstractSignEditScreen)(Object)this;
        var accessor = (AbstractSignEditScreenAccessor) screen;
        Handlers.SignScreenHandler(accessor);
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void complition_assist$onKeyPressed(int keyCode, int scanCode, int modifiers,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (ComplitionAssist.isNotWorking()) return;
        AbstractSignEditScreen screen = (AbstractSignEditScreen)(Object)this;
        var accessor = (AbstractSignEditScreenAccessor) screen;
        Handlers.SignScreenHandler(accessor);
    }

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void complition_assist$charTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ComplitionAssist.isNotWorking()) return;
        AbstractSignEditScreen screen = (AbstractSignEditScreen)(Object)this;
        var accessor = (AbstractSignEditScreenAccessor) screen;
        Handlers.SignScreenHandler(accessor);
    }

    @Inject(method = "renderSignText", at = @At("RETURN"))
    private void complition_assist$renderSignText(DrawContext context, CallbackInfo ci) {
        if (ComplitionAssist.isNotWorking()) return;
        Suggestions.tryRender(context);
    }
}