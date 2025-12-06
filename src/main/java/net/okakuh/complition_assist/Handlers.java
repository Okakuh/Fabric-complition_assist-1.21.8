package net.okakuh.complition_assist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SelectionManager;
import net.okakuh.complition_assist.mixin.AbstractSignEditScreenAccessor;

import java.awt.event.ComponentListener;

import static net.okakuh.complition_assist.Util.parseSequence;

public class Handlers {
    public static void TextFieldWidgetHandler(TextFieldWidget widget) {

        String keyChar = Suggestions.getKeyChar();
        int widgetX = widget.getX();
        int widgetY = widget.getY();
        int widgetHeight = widget.getHeight();
        int widgetCursorPosition = widget.getCursor();
        int widgetWidthBorder = (widget.getWidth() - widget.getInnerWidth()) / 2;

        String widgetText = widget.getText();
        String textBeforeCursor = widgetText.substring(0, widgetCursorPosition);

        String new_sequence = parseSequence(textBeforeCursor, keyChar);
        if (new_sequence.isEmpty()) {
            Suggestions.OFF();
            return;
        }

        // Get new render position
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int X = widgetX + widgetWidthBorder + client.textRenderer.getWidth(textBeforeCursor) - 3;
        int YOffset = widgetHeight / 2 + 1;
        int Y = widgetY + YOffset - 2;

        Suggestions.setNewRenderData(new_sequence, X, Y, YOffset);
        Suggestions.ON(false);
    }

    public static void EditBoxWidgetHandler(EditBoxWidget widget, EditBox editBox) {
        String keyChar = Suggestions.getKeyChar();
        int lineIndex = editBox.getCurrentLineIndex() + 1;
        String widgetText = widget.getText();

        int widgetCursorPosition = editBox.getCursor();
        int widgetX = widget.getX();
        int widgetY = widget.getY();

        String textBeforeCursor = widgetText.substring(0, widgetCursorPosition);

        String new_sequence = parseSequence(textBeforeCursor, keyChar);

        if (new_sequence.isEmpty()) {
            Suggestions.OFF();
            return;
        }

        // Get new render position
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int a = 0;

        for (int i = 0; i < textBeforeCursor.length(); i++) {
            char c = textBeforeCursor.charAt(i);

            int f = client.textRenderer.getWidth(String.valueOf(c));

            if (a + f > 114 || String.valueOf(c).contentEquals("\n")) {
                a = 0;
            }

            a += client.textRenderer.getWidth(String.valueOf(c));
        }

        int X = widgetX + a - 8;
        if (lineIndex == 1) X += 9;

        int YOffset = 4;
        int Y = widgetY + (lineIndex * 9) + 1;
        Suggestions.setNewRenderData(new_sequence, X, Y, YOffset);
        Suggestions.ON(true);
    }

    public static void SignScreenHandler(AbstractSignEditScreenAccessor accessor) {
        String keyChar = Suggestions.getKeyChar();
        int currentRow = accessor.getCurrentRow();
        String[] messages = accessor.getMessages();
        SelectionManager selectionManager = accessor.getSelectionManager();

        String currentLine = messages[currentRow];
        int cursorPos = selectionManager.getSelectionStart();

        String textBeforeCursor = currentLine.substring(0, cursorPos);

        String new_sequence = parseSequence(textBeforeCursor, keyChar);
        if (new_sequence.isEmpty()) {
            Suggestions.OFF();
            return;
        }

        // Get new render position
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int lenPixcCurrLine = client.textRenderer.getWidth(currentLine);
        int lenPixcBeforeCursor = client.textRenderer.getWidth(textBeforeCursor);
        int xCursorOffsetFromLineCenter = (lenPixcCurrLine / 2) - (lenPixcCurrLine - lenPixcBeforeCursor);

        int lineHeight = 10;

        int X = xCursorOffsetFromLineCenter - 3;
        int YOffset = (lineHeight / 2) + 1;
        int Y = ((currentRow + 1) * lineHeight) + YOffset - 32;

        Suggestions.setNewRenderData(new_sequence, X, Y, YOffset);
        Suggestions.ON(false);
    }
}
