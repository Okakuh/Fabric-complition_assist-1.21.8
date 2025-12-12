package net.okakuh.complition_assist.suggesters;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.okakuh.complition_assist.ComplitionAssist;

import java.util.Map;

import static net.okakuh.complition_assist.Util.*;

public class TextFieldWidgetSuggester {
    private final TextFieldWidget widget;
    private RenderData renderData = new RenderData();

    public TextFieldWidgetSuggester(TextFieldWidget widget) {
        this.widget = widget;
        widget.setChangedListener(this::onChanged);
        this.renderData.inRowSuggestionColor = 0xFFa32121;
    }

    private void onChanged(String s) {
        String keyChar = ComplitionAssist.getKeyChar();
        int widgetX = widget.getX();
        int widgetY = widget.getY();
        int widgetHeight = widget.getHeight();
        int widgetCursorPosition = widget.getCursor();
        int widgetWidthBorder = (widget.getWidth() - widget.getInnerWidth()) / 2;

        String widgetText = widget.getText();
        String textBeforeCursor = widgetText.substring(0, widgetCursorPosition);

        String new_sequence = parseSequence(textBeforeCursor, keyChar);
        if (new_sequence.isEmpty()) {
            ComplitionAssist.setSuggesting(false);
            return;
        }

        // Get new render position
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int X = widgetX + widgetWidthBorder + client.textRenderer.getWidth(textBeforeCursor) - 3;
        int YOffset = widgetHeight / 2;
        int Y = widgetY + YOffset;

        int textHeight = 7;
        int suggestionInRowY = widgetY + (int) Math.floor((double) (widgetHeight - textHeight) / 2);

        int minHeightBelowText = 5;
        int spaceBelow = widgetY + widgetHeight - (suggestionInRowY + 7);

        if (spaceBelow < minHeightBelowText)
            suggestionInRowY -= (minHeightBelowText - spaceBelow);

        this.renderData.sequence = new_sequence;
        this.renderData.textCursorX = X;
        this.renderData.textCursorY = Y;
        this.renderData.suggestions_Y_offset = YOffset;
        this.renderData.suggestionInRowY = suggestionInRowY;

        Map<String, String> suggestionsALL = ComplitionAssist.getSuggestions();

        this.renderData.suggestionsForSequence = parseSuggestions(new_sequence, suggestionsALL);
        this.renderData.displaySuggestionsForSequence = parseDisplaySuggestions(this.renderData.suggestionsForSequence,
                suggestionsALL);

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
        text = text.replaceAll("§[^\\s]?", "");
        this.widget.setText(text);
    }
}