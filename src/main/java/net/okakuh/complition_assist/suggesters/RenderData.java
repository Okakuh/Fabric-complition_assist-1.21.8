package net.okakuh.complition_assist.suggesters;

import java.util.ArrayList;
import java.util.List;

public class RenderData {
    public int cursorX = 0;
    public int cursorY = 0;
    public int yOffset = 0;
    public int sTextY = 0;
    public String sequence = "";

    public List<String> suggestionsForSequence = new ArrayList<>();
    public List<String> displaySuggestionsForSequence = new ArrayList<>();

    public int inRowSuggestionColor = 0;
    public RenderData(){};
}
