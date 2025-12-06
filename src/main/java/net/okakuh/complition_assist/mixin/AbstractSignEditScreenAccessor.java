package net.okakuh.complition_assist.mixin;

import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
    @Accessor("selectionManager")
    SelectionManager getSelectionManager();

    @Accessor("currentRow")
    int getCurrentRow();

    @Accessor("messages")
    String[] getMessages();
}