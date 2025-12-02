package net.okakuh.complition_assist.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Keyboard.class)
public interface KeyboardAccessor {
    @Invoker("onKey")
    void invokeOnKey(long window, int key, int scancode, int action, int modifiers);

    @Invoker("onChar")
    void invokeOnChar(long window, int codePoint, int modifiers);
}