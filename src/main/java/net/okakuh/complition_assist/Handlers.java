package net.okakuh.complition_assist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SelectionManager;
import net.okakuh.complition_assist.mixin.AbstractSignEditScreenAccessor;

import java.util.regex.*;

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
        int YOffset = widgetHeight / 2;
        int Y = widgetY + YOffset;
        int textY = widgetY + (int) Math.floor((double) (widgetHeight - 7) / 2);

        int minHeightBelow = 5;
        int spaceBelow = widgetY + widgetHeight - (textY + 7);

        if (spaceBelow < minHeightBelow)
            textY -= (minHeightBelow - spaceBelow);

        Suggestions.setNewRenderData(new_sequence, X, Y, YOffset, textY, 0xFF333333);
        Suggestions.ON(false);
    }

    public static void EditBoxWidgetHandler(EditBoxWidget widget, EditBox editBox) {
        String keyChar = Suggestions.getKeyChar();

        int currentCursorLineIndex = editBox.getCurrentLineIndex();
        String line = String.valueOf(editBox.getLine(currentCursorLineIndex));
        Pattern pattern = Pattern.compile("beginIndex=(\\d+), endIndex=(\\d+)");
        Matcher matcher = pattern.matcher(line);

        int rowBeginIndex = 0;
        if (matcher.find()) {
            rowBeginIndex = Integer.parseInt(matcher.group(1));
        }

        String widgetText = widget.getText();
        int widgetCursorPosition = editBox.getCursor();

        String textBeforeCursor = widgetText.substring(rowBeginIndex, widgetCursorPosition);
        String new_sequence = parseSequence(textBeforeCursor, keyChar);

        if (new_sequence.isEmpty()) {
            Suggestions.OFF();
            return;
        }

        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        // Высчитываем позицию курсора
        int widgetX = widget.getX();
        int widgetY = widget.getY();


        int X = widgetX + client.textRenderer.getWidth(textBeforeCursor) + 1;
        int YOffset = 5;
        int Y = widgetY + ((currentCursorLineIndex + 1) * 9);

        Suggestions.setNewRenderData(new_sequence, X, Y, YOffset, Y - 5, 0xFFaaadab);
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

        Suggestions.setNewRenderData(new_sequence, X, Y, YOffset, Y-4, 0xFF333333);
        Suggestions.ON(false);
    }
}
