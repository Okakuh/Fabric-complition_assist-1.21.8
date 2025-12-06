package net.okakuh.complition_assist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.okakuh.complition_assist.Util.parseDisplaySuggestions;
import static net.okakuh.complition_assist.Util.parseSuggestions;

public class Suggestions {
    // Все загруженные подсказки
    private static final Map<String, String> suggestionsALL = new HashMap<>();
    // Символ который используеться для определения начала последовательности символов
    private static final String keyChar = ":";

    // Состояния подсказочника
    private static boolean SUGGEST = false;

    // Какой источник DrawContext нужно использовать для рендера
    private static boolean shouldUseScreenDrawContext = false;

    // Подсказки
    private static String sequence = "";
    private static List<String> suggestionsForSequence = new ArrayList<>();
    private static List<String> displaySuggestionsForSequence = new ArrayList<>();

    // Инфа для рендера

    // Позиция подсказок
    private static int cordX = 0;
    private static int cordY = 0;
    private static int WIDTH = 0;
    private static int HEIGHT = 0;

    // Отступы, ширина гранцы и высота строки подсказок
    private static final int borderPadding = 2;
    private static final int borderWidth = 1;
    private static final int lineHeight = 10;

    // Цвета
    private static final int borderColor = 0xFFaba9a2;
    private static final int textColor = 0xFFe37d10;
    private static final int backgroundColor = 0xB3000000;

    public static void add(String key, String value) {
        suggestionsALL.put(key, value);
    }

    public static void clear() {
        suggestionsALL.clear();
    }

    public static void OFF() {
        shouldUseScreenDrawContext = false;
        SUGGEST = false;
    }

    public static void ON(boolean useScreenDrawContext) {
        shouldUseScreenDrawContext = useScreenDrawContext;
        SUGGEST = true;
    }

    public static boolean isON() {
        return SUGGEST;
    }

    public static boolean isUsingScreenDrawContext() {
        return shouldUseScreenDrawContext;
    }

    public static String getReplacement() {
        return suggestionsALL.get(suggestionsForSequence.getFirst());
    }

    public static String getSequence() {return sequence;}

    public static String getKeyChar() {
        return keyChar;
    }

    public static void setNewRenderData(String newSequence, int X, int Y, int YOffset) {
        sequence = newSequence;
        suggestionsForSequence = parseSuggestions(sequence, suggestionsALL);
        displaySuggestionsForSequence = parseDisplaySuggestions(suggestionsForSequence, suggestionsALL);

        // Координата Х для рендера подсказок
        cordX = X;
        // Координата У для рендера подсказок
        cordY = Y;

        // Определение высоты и ширины с фоном и границей
        // Высота
        int suggestionCount = displaySuggestionsForSequence.size();
        int suggestionsHeight = suggestionCount * lineHeight;

        // Ширина
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;
        int maxWidth = 0;
        for (String displayText : displaySuggestionsForSequence) {
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(displayText));
        }

        HEIGHT = suggestionsHeight + (borderPadding * 2) + (borderWidth * 2);
        WIDTH = maxWidth + (borderPadding * 2) + (borderWidth * 2);

        // Сдвиг по Х для рендера подсказок красиво символ в символ под текстом
        cordX -= client.textRenderer.getWidth(sequence);

        // Добавляем защиту от рендера вне экрана

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // По высоте
        if ((screenHeight - (cordY + YOffset)) > HEIGHT) {
            cordY += YOffset;
        } else {
            cordY -= (YOffset + HEIGHT);
            // Отзеркаливаем список если будем рендерить подсказки сверху
            // Чтобы первое предложение на замену было снизу
            displaySuggestionsForSequence = displaySuggestionsForSequence.reversed();
        }
        // По ширине
        if (cordX + WIDTH > screenWidth) cordX -= WIDTH;
    }

    public static void tryRender(DrawContext context) {
        if (SUGGEST) {
            if (displaySuggestionsForSequence == null || displaySuggestionsForSequence.isEmpty()) return;
            render(context);
        }
    }

    private static void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        // Рендер фона
        context.fill(cordX, cordY, cordX + WIDTH, cordY + HEIGHT, backgroundColor);
        // Рендер гриницы
        context.drawBorder(cordX, cordY, WIDTH, HEIGHT, borderColor);

        // Стдвиг текста относительно начала рендера с учетом ширины гранцицы и отступа от границы
        int textY = cordY + borderPadding + borderWidth;
        int textX = cordX + borderPadding + borderWidth;

        // Рендер текста
        for (String displayText : displaySuggestionsForSequence) {
            context.drawText(client.textRenderer, displayText, textX, textY, textColor, true);
            textY += lineHeight;
        }
    }
}
