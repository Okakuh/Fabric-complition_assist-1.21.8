    package net.okakuh.complition_assist;

    import net.minecraft.client.MinecraftClient;
    import net.minecraft.client.gui.DrawContext;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    import static net.okakuh.complition_assist.Util.*;

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

        private static int cursorX = 0;
        private static int cursorY = 0;

        // Инфа для рендера

        // Позиция подсказок
        private static boolean isDisplaySuggestionsMirrored = false;
        private static int SUGGESTIONS_X = 0;
        private static int SUGGESTIONS_Y = 0;
        private static int SUGGESTIONS_WIDTH = 0;
        private static int SUGGESTIONS_HEIGHT = 0;
        private static int WIDGET_HEIGHT_OFFSET = 0;

        // Отступы, ширина гранцы и высота строки подсказок
        private static final int borderPadding = 2;
        private static final int borderWidth = 1;
        private static final int lineHeight = 10;

        // Цвета
        private static final int borderColor = 0xFFaba9a2;
        private static final int textColor = 0xFFFFFFFF;
        private static final int inRowTextColor = 0xFF333333;
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

        public static void setNewRenderData(String newSequence, int X, int Y, int widgetYOffset) {
            cursorX = X;
            cursorY = Y;
            WIDGET_HEIGHT_OFFSET = widgetYOffset;

            sequence = newSequence;
            suggestionsForSequence = parseSuggestions(sequence, suggestionsALL);
            displaySuggestionsForSequence = parseDisplaySuggestions(suggestionsForSequence, suggestionsALL);

            // Координата Х для рендера подсказок
            SUGGESTIONS_X = X;
            // Координата У для рендера подсказок
            SUGGESTIONS_Y = Y;

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

            SUGGESTIONS_HEIGHT = suggestionsHeight + (borderPadding * 2) + (borderWidth * 2);
            SUGGESTIONS_WIDTH = maxWidth + (borderPadding * 2) + (borderWidth * 2);

            // Сдвиг по Х для рендера подсказок красиво символ в символ под текстом
            SUGGESTIONS_X -= client.textRenderer.getWidth(sequence);

            // Добавляем защиту от рендера вне экрана

            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // По высоте
            if ((screenHeight - (SUGGESTIONS_Y + WIDGET_HEIGHT_OFFSET)) > SUGGESTIONS_HEIGHT) {
                SUGGESTIONS_Y += WIDGET_HEIGHT_OFFSET;
                isDisplaySuggestionsMirrored = false;

            } else {
                isDisplaySuggestionsMirrored = true;
                SUGGESTIONS_Y -= WIDGET_HEIGHT_OFFSET + SUGGESTIONS_HEIGHT;

                // Отзеркаливаем список если будем рендерить подсказки сверху
                // Чтобы первое предложение на замену было снизу
                displaySuggestionsForSequence = displaySuggestionsForSequence.reversed();
            }
            // По ширине
            if (SUGGESTIONS_X + SUGGESTIONS_WIDTH > screenWidth) SUGGESTIONS_X -= SUGGESTIONS_WIDTH;
        }

        private static void render(DrawContext context) {
            var client = MinecraftClient.getInstance();
            // Рендер фона
            context.fill(SUGGESTIONS_X, SUGGESTIONS_Y, SUGGESTIONS_X + SUGGESTIONS_WIDTH,
                    SUGGESTIONS_Y + SUGGESTIONS_HEIGHT, backgroundColor);
            // Рендер гриницы
            context.drawBorder(SUGGESTIONS_X, SUGGESTIONS_Y, SUGGESTIONS_WIDTH, SUGGESTIONS_HEIGHT, borderColor);


            // Стдвиг текста относительно начала рендера с учетом ширины границы и отступа от границы
            int textY = SUGGESTIONS_Y + borderPadding + borderWidth;
            int textX = SUGGESTIONS_X + borderPadding + borderWidth;

            int index = 0;

            if (isDisplaySuggestionsMirrored) {
                index = displaySuggestionsForSequence.size() - 1;
            }

            String inRowSuggestion = displaySuggestionsForSequence.get(index);
            String stripedInRowSuggestion = inRowSuggestion.substring(sequence.length());

            context.drawText(client.textRenderer,
                    stripedInRowSuggestion, cursorX + 3, cursorY - 6, inRowTextColor, true);

            // Рендер текста
            for (String displayText : displaySuggestionsForSequence) {
                context.drawText(client.textRenderer, displayText, textX, textY, textColor, true);

                textY += lineHeight;
            }
        }

        public static void tryRender(DrawContext context) {
            if (SUGGEST) {
                if (displaySuggestionsForSequence == null || displaySuggestionsForSequence.isEmpty()) return;
                render(context);
            }
        }

        public static void onMouseClick(double mouseX, double mouseY) {
            int extra = borderWidth + borderPadding;
            int Y2 = SUGGESTIONS_Y + SUGGESTIONS_HEIGHT - extra;

            if (SUGGESTIONS_X + extra > mouseX || mouseX > SUGGESTIONS_X + SUGGESTIONS_WIDTH - extra) return;
            if (SUGGESTIONS_Y + extra > mouseY || mouseY > Y2) return;

            float OffsetFromBottom = (float) (Y2 - mouseY) / lineHeight;

            int index = (int) Math.floor(OffsetFromBottom);
            if (!isDisplaySuggestionsMirrored)
                index = suggestionsForSequence.size() - (index + 1);

            MinecraftClient client = MinecraftClient.getInstance();

            String replacement = suggestionsALL.get(suggestionsForSequence.get(index));

            processReplacement(client, sequence, replacement);
        }
    }
