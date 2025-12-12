package net.okakuh.complition_assist.suggesters;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.EditBox;
import net.okakuh.complition_assist.ComplitionAssist;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.okakuh.complition_assist.Util.*;

public class EditBoxSuggester {
    private final EditBoxWidget widget;
    private final EditBox editBox;
    private final RenderData renderData = new RenderData();

    public EditBoxSuggester(EditBoxWidget widget, EditBox editbox) {
        this.widget = widget;
        this.editBox = editbox;
        this.editBox.setChangeListener(this::onChanged);
        this.renderData.inRowSuggestionColor = 0xFFaaadab;
    }

    private void onChanged(String s) {
        String keyChar = ComplitionAssist.getKeyChar();

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
            ComplitionAssist.setSuggesting(false);
            return;
        }

        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int widgetX = widget.getX();
        int widgetY = widget.getY();

        int X = widgetX + client.textRenderer.getWidth(textBeforeCursor) + 1;
        int YOffset = 5;
        int Y = widgetY + ((currentCursorLineIndex + 1) * 9);

        this.renderData.sequence = new_sequence;
        this.renderData.textCursorX = X;
        this.renderData.textCursorY = Y;
        this.renderData.suggestions_Y_offset = YOffset;
        this.renderData.suggestionInRowY = Y - 5;

        Map<String, String> suggestionsALL = ComplitionAssist.getSuggestions();

        this.renderData.suggestionsForSequence = parseSuggestions(new_sequence, suggestionsALL);
        this.renderData.displaySuggestionsForSequence = parseDisplaySuggestions(this.renderData.suggestionsForSequence, suggestionsALL);

        if (this.renderData.displaySuggestionsForSequence.isEmpty()) {
            ComplitionAssist.setSuggesting(false);
            return;
        }

        ComplitionAssist.setNewRenderData(this.renderData);
        ComplitionAssist.setNewReplacer(this::replacer);
        ComplitionAssist.setSuggesting(true);
    }

    public void replacer(int i) {
        Map<String, String> suggestionsALL = ComplitionAssist.getSuggestions();
        String sequence = this.renderData.sequence;
        String replacement = suggestionsALL.get(renderData.suggestionsForSequence.get(i));
        sequence = ComplitionAssist.getKeyChar() + sequence;

        String text = this.widget.getText();
        text = text.replace((CharSequence) sequence, (CharSequence) replacement);
        this.widget.setText(text);
    }
}