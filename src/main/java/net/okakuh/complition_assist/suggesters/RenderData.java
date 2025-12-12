package net.okakuh.complition_assist.suggesters;

import java.util.ArrayList;
import java.util.List;

public class RenderData {
    public int textCursorX = 0;
    public int textCursorY = 0;
    public int suggestions_Y_offset = 0;
    public int suggestionInRowY = 0;
    public String sequence = "";

    public List<String> suggestionsForSequence = new ArrayList<>();
    public List<String> displaySuggestionsForSequence = new ArrayList<>();

    public int inRowSuggestionColor = 0xFFFFFFFF;
    public RenderData(){};
}
