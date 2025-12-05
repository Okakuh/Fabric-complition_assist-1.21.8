package net.okakuh.complition_assist;

import com.google.gson.JsonElement;
import com.ibm.icu.impl.locale.LocaleObjectCache;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.EditBox;

import net.minecraft.client.util.SelectionManager;
import net.minecraft.resource.ResourceType;
import net.okakuh.complition_assist.mixin.AbstractSignEditScreenAccessor;
import net.okakuh.complition_assist.mixin.KeyboardAccessor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;


import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.InputStreamReader;
import java.util.List;

import java.util.*;

public class ComplitionAssist implements ClientModInitializer {
    public static final String MOD_ID = "complition_assist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier SHORTCUTS_ID = Identifier.of("complition_assist", "shortcuts.json");

    private static final Map<String, String> SHORTCUTS = new HashMap<>();

    private static final String keyChar = ":";

    private static boolean SUGGEST = false;
    private static String currentSequence = "";
    private static boolean shouldUseScreenContext = false;

    private static List<String> currentSuggestions = new ArrayList<>();
    private static List<String> displaySuggestions = new ArrayList<>();

    private static int suggestionXpos = 0;
    private static int suggestionYpos = 0;
    private static int suggestionYcorrection = 0;

    @Override
    public void onInitializeClient() {
        initializeDefaultShortcuts();

        // Регистрация ResourceReloadListener
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("complition_assist", "shortcuts_loader");
                    }

                    @Override
                    public void reload(ResourceManager resourceManager) {
                        loadFromResourcePacks(resourceManager);
                    }
                }
        );
    }

    private void initializeDefaultShortcuts() {
        SHORTCUTS.put("пкд", "Привет как дела?");
        SHORTCUTS.put("гг", "Хорошей игры!");
    }

    public static void onKeyPresed(int keyCode, int modifiers) {
        boolean shiftPressed = (modifiers & 1) != 0;
        boolean spacePressed = keyCode == 32;

        if (SUGGEST) {
            if (shiftPressed && spacePressed) {
                String suggestion = currentSuggestions.getFirst();
                String replacement = getShortcutValue(suggestion);
                if (replacement != null) {
                    processReplacement(MinecraftClient.getInstance(), replacement);
                }
            }
        }
    }

    public static void render(DrawContext context, boolean isScrenContext) {
        if (SUGGEST) {
            if (isScrenContext == shouldUseScreenContext) renderSuggestions(context);
        }
    }

    private static void parseSuggestions() {
        Set<String> startsWith = new LinkedHashSet<>();
        Set<String> containsOnly = new LinkedHashSet<>();

        String inputLower = currentSequence.toLowerCase();

        // Один проход по всем сокращениям
        for (String shortcut : SHORTCUTS.keySet()) {
            String shortcutLower = shortcut.toLowerCase();

            if (shortcutLower.startsWith(inputLower)) {
                startsWith.add(shortcut);
            } else if (shortcutLower.contains(inputLower)) {
                containsOnly.add(shortcut);
            }
        }

        // Объединяем: сначала startsWith, потом containsOnly
        List<String> result = new ArrayList<>();
        result.addAll(startsWith);
        result.addAll(containsOnly);

        // Сортируем внутри каждой группы

        // Сортируем первую группу (startsWith)
        List<String> sortedStartsWith = new ArrayList<>(startsWith);
        sortedStartsWith.sort(String::compareToIgnoreCase);
        List<String> sortedResult = new ArrayList<>(sortedStartsWith);

        // Сортируем вторую группу (containsOnly)
        List<String> sortedContainsOnly = new ArrayList<>(containsOnly);
        sortedContainsOnly.sort(String::compareToIgnoreCase);
        sortedResult.addAll(sortedContainsOnly);

        // Ограничиваем количество
        if (sortedResult.size() > 10) {
            currentSuggestions = sortedResult.subList(0, 10);
        } else {
            currentSuggestions = sortedResult;
        }
    }

    private static void parseDisplaySuggestions() {
        List<String> displayList = new ArrayList<>();

        for (String suggestion : currentSuggestions) {
            String fullText = getShortcutValue(suggestion);
            if (fullText != null) {
                String displayText = suggestion + " → " + fullText;
                displayList.add(displayText);
            }
        }

        displaySuggestions = displayList;
    }

    private static String getShortcutValue(String shortcut) {
        return SHORTCUTS.get(shortcut.toLowerCase());
    }

    private static void loadFromResourcePacks(ResourceManager resourceManager) {
        // Сохраняем дефолтные сокращения
        Map<String, String> defaultShortcuts = new HashMap<>(SHORTCUTS);
        SHORTCUTS.clear();
        SHORTCUTS.putAll(defaultShortcuts);

        // Загружаем из всех ресурспаков
        try {
            List<Resource> resources = resourceManager.getAllResources(SHORTCUTS_ID);

            for (Resource resource : resources) {
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);

                    if (json.has("shortcuts")) {
                        JsonObject shortcutsObj = json.getAsJsonObject("shortcuts");

                        for (Map.Entry<String, JsonElement> entry : shortcutsObj.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue().getAsString();
                            SHORTCUTS.put(key.toLowerCase(), value);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading shortcuts from {}", resource.getPack(), e);
                }
            }

            LOGGER.info("Loaded {} shortcuts from resource packs", SHORTCUTS.size() - defaultShortcuts.size());

        } catch (Exception e) {
            LOGGER.error("Error loading shortcuts", e);
        }
    }

    private static String parseSequence(String text) {
        int colonIndex = text.lastIndexOf(keyChar);
        if (colonIndex == -1) return "";

        // Получаем текст между знаком : и концом строки (до курсора)
        String betweenColonAndEnd = text.substring(colonIndex + 1);

        // Проверяем есть ли пробелы между знаком : и концом строки
        if (betweenColonAndEnd.contains(" ") || betweenColonAndEnd.contains("\n")) return "";

        // Возвращаем символы от знака : до конца строки (до курсора)
        return betweenColonAndEnd.toLowerCase();
    }

    public static void trigerTextFieldWidget(TextFieldWidget widget) {
        shouldUseScreenContext = false;
        int widgetX = widget.getX();
        int widgetY = widget.getY();
        int widgetHeight = widget.getHeight();
        int widgetCursorPosition = widget.getCursor();
        int widgetWidthBorder = (widget.getWidth() - widget.getInnerWidth()) / 2;

        String widgetText = widget.getText();
        String textBeforeCursor = widgetText.substring(0, widgetCursorPosition);

        String new_sequence = parseSequence(textBeforeCursor);
        if (new_sequence.isEmpty()) {
            SUGGEST = false;
            return;
        }
        currentSequence = new_sequence;

        // Get new render position
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        suggestionXpos = widgetX + widgetWidthBorder + client.textRenderer.getWidth(textBeforeCursor) - client.textRenderer.getWidth(currentSequence) - 3;
        suggestionYcorrection = widgetHeight / 2 + 1;
        suggestionYpos = widgetY - 2 + suggestionYcorrection;

        parseSuggestions();
        parseDisplaySuggestions();

        SUGGEST = true;
    }

    public static void trigerEditBoxWidget(EditBoxWidget widget, EditBox editBox) {
        int lineIndex = editBox.getCurrentLineIndex() + 1;
        String widgetText = widget.getText();

        int widgetCursorPosition = editBox.getCursor();
        int widgetX = widget.getX();
        int widgetY = widget.getY();

        String textBeforeCursor = widgetText.substring(0, widgetCursorPosition);

        String new_sequence = parseSequence(textBeforeCursor);

        if (new_sequence.isEmpty()) {
            SUGGEST = false;
            return;
        }

        currentSequence = new_sequence;

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

        suggestionXpos = widgetX + a - client.textRenderer.getWidth(new_sequence) - 8;
        if (lineIndex == 1) suggestionXpos += 9;

        suggestionYcorrection = 4;
        suggestionYpos = widgetY + (lineIndex * 9) + 1;

        parseSuggestions();
        parseDisplaySuggestions();

        SUGGEST = true;
        shouldUseScreenContext = true;
    }

    public static void trigerSign(AbstractSignEditScreenAccessor accessor) {
        shouldUseScreenContext = false;
        int currentRow = accessor.getCurrentRow();
        String[] messages = accessor.getMessages();
        SelectionManager selectionManager = accessor.getSelectionManager();

        String currentLine = messages[currentRow];
        int cursorPos = selectionManager.getSelectionStart();

        String textBeforeCursor = currentLine.substring(0, cursorPos);

        String new_sequence = parseSequence(textBeforeCursor);
        if (new_sequence.isEmpty()) {
            SUGGEST = false;
            return;
        }
        currentSequence = new_sequence;

        // Get new render position
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int lenPixcCurrLine = client.textRenderer.getWidth(currentLine);
        int lenPixcBeforeCursor = client.textRenderer.getWidth(textBeforeCursor);
        int xCursorOffsetFromLineCenter = (lenPixcCurrLine / 2) - (lenPixcCurrLine - lenPixcBeforeCursor);

        int lineHeight = 10;
        suggestionXpos = xCursorOffsetFromLineCenter - client.textRenderer.getWidth(currentSequence) - 3;
        suggestionYcorrection = (lineHeight / 2) + 1;
        suggestionYpos = ((currentRow + 1) * lineHeight) + suggestionYcorrection - 32;

        parseSuggestions();
        parseDisplaySuggestions();

        SUGGEST = true;
    }

    private static void renderSuggestions(DrawContext context) {
        if (displaySuggestions == null || displaySuggestions.isEmpty()) return;

        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        int cursorX = suggestionXpos;
        int cursorY = suggestionYpos;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int suggestionCount = displaySuggestions.size();
        int lineHeight = 10;
        int textHight = suggestionCount * lineHeight;

        int borderColor = 0xFFaba9a2;
        int textColor = 0xFFe37d10;
        int backgroundColor = 0xB3000000;
        int borderPadding = 2;
        int borderWidth = 1;

        int totalHeight = textHight + borderPadding * 2 + borderWidth * 2;

        int startYBelow = cursorY + suggestionYcorrection;
        int availableSpaceBelow = screenHeight - startYBelow;

        int startYAbove = cursorY - suggestionYcorrection - totalHeight;

        int startY;

        if (availableSpaceBelow >= totalHeight) {
            startY = startYBelow;
        } else if (startYAbove >= totalHeight) {
            startY = startYAbove;
        } else {
            startY = (availableSpaceBelow >= startYAbove) ? startYBelow : startYAbove;
        }

        int maxWidth = 0;
        for (String displayText : displaySuggestions) {
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(displayText));
        }

        if (maxWidth == 0) return;

        if (cursorX + maxWidth + 10 > screenWidth) {
            cursorX = Math.max(10, cursorX - maxWidth - 10);
        }

        int totalWidth = maxWidth + borderPadding * 2 + borderWidth * 2;

        int bgX1 = cursorX;
        int bgY1 = startY;
        int bgX2 = cursorX + totalWidth;
        int bgY2 = startY + totalHeight;

        context.fill(bgX1, bgY1, bgX2, bgY2, backgroundColor);
        context.drawBorder(bgX1, bgY1, totalWidth, totalHeight, borderColor);

        int textY = startY + borderPadding + borderWidth;
        int textX = cursorX + borderPadding + borderWidth;

        List<String> renderDisplaySuggestions;

        if (startY == startYAbove) {
            renderDisplaySuggestions = displaySuggestions.reversed();
        } else {
            renderDisplaySuggestions = displaySuggestions;
        }

        for (String displayText : renderDisplaySuggestions) {
            context.drawText(client.textRenderer, displayText, textX, textY, textColor, true);

            textY += lineHeight;
        }
    }

    public static void suggestionsOFF() {
        shouldUseScreenContext = false;
        SUGGEST = false;
    }

    private static void processReplacement(MinecraftClient client, String replacement) {
        // Симулируем Backspace для удаления двоеточия и последовательности
        simulateBackspaces(client, currentSequence.length() + 1);

        // Вставляем замену
        simulateTextInput(client, replacement);
    }

    private static void simulateBackspaces(MinecraftClient client, int count) {
        KeyboardAccessor keyboard = (KeyboardAccessor) client.keyboard;
        long window = client.getWindow().getHandle();

        for (int i = 0; i < count; i++) {
            keyboard.invokeOnKey(window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);
            keyboard.invokeOnKey(window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_RELEASE, 0);
        }
    }

    private static void simulateTextInput(MinecraftClient client, String text) {
        KeyboardAccessor keyboard = (KeyboardAccessor) client.keyboard;
        long window = client.getWindow().getHandle();

        for (char c : text.toCharArray()) {
            keyboard.invokeOnChar(window, (int) c, 0);
        }
    }
}