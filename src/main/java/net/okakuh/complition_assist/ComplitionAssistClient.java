package net.okakuh.complition_assist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.resource.ResourceType;
import net.okakuh.complition_assist.mixin.KeyboardAccessor;
import org.joml.Vector2i;
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

import java.lang.reflect.Method;
import java.util.*;

public class ComplitionAssistClient implements ClientModInitializer {
    public static final String MOD_ID = "complition_assist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier SHORTCUTS_ID = Identifier.of("complition_assist", "shortcuts.json");

    private static final Map<String, String> SHORTCUTS = new HashMap<>();

    // Состояние отслеживания
    private static boolean isTracking = false;
    private static StringBuilder currentSequence = new StringBuilder();

    private static List<String> currentSuggestions = new ArrayList<>();

    // Для отслеживания двоеточия
    private static boolean colonDetected = false;

    // Для обработки замены
    private static ReplacementTask pendingReplacement = null;

    private static class ReplacementTask {
        String sequence;
        String replacement;

        ReplacementTask(String sequence, String replacement) {
            this.sequence = sequence;
            this.replacement = replacement;
        }
    }

    @Override
    public void onInitializeClient() {
        // Регистрация ResourceReloadListener
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("complition_assist", "shortcuts_loader");
                    }

                    @Override
                    public void reload(ResourceManager resourceManager) {
                        ComplitionAssistClient.loadFromResourcePacks(resourceManager);
                    }
                }
        );

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (isTracking && !currentSuggestions.isEmpty()) {
                InputFieldTracker.update();
                if (InputFieldTracker.hasActiveField()) {
                    Vector2i pos = InputFieldTracker.getFieldPosition();
                    renderSuggestionsHud(drawContext, pos.x, pos.y);
                }
            }
        });

        // Регистрация других обработчиков
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick(client));
    }

    private void onClientTick(MinecraftClient client) {
        // Обрабатываем обнаруженное двоеточие
        if (colonDetected) {
            colonDetected = false;
            if (!isTracking) {
                startTracking();
            }
        }

        // Обрабатываем ожидающую замену
        if (pendingReplacement != null) {
            processReplacement(client);
            pendingReplacement = null;
        }

        if (isTracking) {
            // Обновляем активное поле
            InputFieldTracker.update();

            // Получаем предложения
            currentSuggestions = getSuggestions(currentSequence.toString());
        }
    }

    // Этот метод нужно будет вызывать из Mixin при вводе символов
    public static void onCharTyped(char character) {
        // Проверяем двоеточие
        if (character == ':') {
            // Сбрасываем предыдущую строку и начинаем запись заново
            resetTracking();
            colonDetected = true;
            return;
        }

        // Если ведем отслеживание, добавляем символы
        if (isTracking) {
            // Проверяем максимальную длину (20 символов)
            if (currentSequence.length() >= 20) {
                resetTracking();
                return;
            }

            // Добавляем символ в последовательность
            currentSequence.append(character);
        }
    }

    // Этот метод нужно будет вызывать из Mixin при нажатии специальных клавиш
    public static void onKeyPressed(int keyCode, int modifiers) {
        // Проверяем Shift+Пробел (Shift = 1, Пробел = 32)
        boolean shiftPressed = (modifiers & 1) != 0; // GLFW.GLFW_MOD_SHIFT = 1
        boolean spacePressed = keyCode == 32; // GLFW.GLFW_KEY_SPACE = 32

        if (shiftPressed && spacePressed) {
            if (isTracking) {
                processTabReplacement();
            }
            return;
        }

        // Если просто пробел без Shift - добавляем его в последовательность
        if (keyCode == 32 && !shiftPressed) {
            if (isTracking) {
                // Проверяем максимальную длину
                if (currentSequence.length() >= 20) {
                    resetTracking();
                    return;
                }
                currentSequence.append(' ');
            }
            return;
        }

        if (!isTracking) return;

        // Проверяем Backspace
        if (keyCode == 259) { // GLFW.GLFW_KEY_BACKSPACE
            if (currentSequence.length() > 0) {
                currentSequence.deleteCharAt(currentSequence.length() - 1);
            } else {
                // Backspace на пустой последовательности после двоеточия - сбрасываем
                resetTracking();
            }
            return;
        }

        // Escape - сбрасываем отслеживание
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            resetTracking();
            return;
        }
    }

    private static void startTracking() {
        isTracking = true;
        currentSequence = new StringBuilder();
    }

    private static void resetTracking() {
        isTracking = false;
        currentSequence = new StringBuilder();
    }

    private static void processTabReplacement() {
        if (currentSuggestions == null || currentSuggestions.isEmpty()) {
            resetTracking();
            return;
        }

        // Берем первый предложенный вариант
        String suggestion = currentSuggestions.get(0);
        String replacement = SHORTCUTS.get(suggestion.toLowerCase());

        if (replacement != null) {
            // Создаем задачу на замену
            pendingReplacement = new ReplacementTask(currentSequence.toString(), replacement);

            // Сбрасываем отслеживание
            resetTracking();
        } else {
            resetTracking();
        }
    }

    private void processReplacement(MinecraftClient client) {
        if (pendingReplacement == null) return;

        try {
            // Симулируем Backspace для удаления двоеточия и последовательности
            simulateBackspaces(client, pendingReplacement.sequence.length() + 2);

            // Вставляем замену
            simulateTextInput(client, pendingReplacement.replacement);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void simulateBackspaces(MinecraftClient client, int count) {
        KeyboardAccessor keyboard = (KeyboardAccessor) client.keyboard;
        long window = client.getWindow().getHandle();

        for (int i = 0; i < count; i++) {
            keyboard.invokeOnKey(window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);
            keyboard.invokeOnKey(window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_RELEASE, 0);
        }
    }

    private void simulateTextInput(MinecraftClient client, String text) {
        KeyboardAccessor keyboard = (KeyboardAccessor) client.keyboard;
        long window = client.getWindow().getHandle();

        for (char c : text.toCharArray()) {
            keyboard.invokeOnChar(window, (int)c, 0);
        }
    }

    private static List<String> getSuggestions(String input) {
        Set<String> startsWith = new LinkedHashSet<>();
        Set<String> containsOnly = new LinkedHashSet<>();

        if (input.isEmpty()) {
            return new ArrayList<>();
        }

        String inputLower = input.toLowerCase();

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
        List<String> sortedResult = new ArrayList<>();

        // Сортируем первую группу (startsWith)
        List<String> sortedStartsWith = new ArrayList<>(startsWith);
        sortedStartsWith.sort(String::compareToIgnoreCase);
        sortedResult.addAll(sortedStartsWith);

        // Сортируем вторую группу (containsOnly)
        List<String> sortedContainsOnly = new ArrayList<>(containsOnly);
        sortedContainsOnly.sort(String::compareToIgnoreCase);
        sortedResult.addAll(sortedContainsOnly);

        // Ограничиваем количество
        if (sortedResult.size() > 10) {
            return sortedResult.subList(0, 10);
        }

        return sortedResult;
    }

    // Геттер для значения сокращения
    public static String getShortcutValue(String shortcut) {
        return SHORTCUTS.get(shortcut.toLowerCase());
    }

    public static void renderSuggestionsHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        if (currentSuggestions == null || currentSuggestions.isEmpty()) {
            return;
        }

        net.minecraft.client.font.TextRenderer textRenderer = client.textRenderer;

        // Получаем активное поле
        TextFieldWidget activeField = InputFieldTracker.getActiveField();
        if (activeField == null) return;

        int fieldX = activeField.getX();
        int fieldY = activeField.getY();
        int fieldHeight = activeField.getHeight();

        // Всегда показываем НАД полем
        int suggestionCount = currentSuggestions.size();
        int lineHeight = 12;
        int totalHeight = suggestionCount * lineHeight;
        int padding = 5;

        int startY = fieldY - totalHeight - padding;

        // Если не хватает места сверху, показываем под полем
        if (startY < 5) {
            startY = fieldY + fieldHeight + padding;
        }

        // Собираем текст для отображения
        List<String> displayTexts = new ArrayList<>();
        int maxWidth = 0;

        for (String suggestion : currentSuggestions) {
            String fullText = getShortcutValue(suggestion);
            if (fullText == null) continue;
            String displayText = suggestion + " → " + fullText;
            displayTexts.add(displayText);
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(displayText));
        }

        if (maxWidth == 0) return;

        // Рисуем фон
        int bgX1 = fieldX - 4;
        int bgY1 = startY - 2;
        int bgX2 = fieldX + maxWidth + 6;
        int bgY2 = startY + totalHeight + 2;

        context.fill(bgX1, bgY1, bgX2, bgY2, 0x80000000);
        context.drawBorder(bgX1, bgY1, maxWidth + 8, totalHeight + 4, 0xFFFFFFFF);

        // Рисуем текст С ОБВОДКОЙ для максимальной видимости
        int textY = startY;
        for (String displayText : displayTexts) {
            // 1. Черная обводка (4 стороны)
            context.drawText(textRenderer, displayText, fieldX - 1, textY, 0xFF000000, false);
            context.drawText(textRenderer, displayText, fieldX + 1, textY, 0xFF000000, false);
            context.drawText(textRenderer, displayText, fieldX, textY - 1, 0xFF000000, false);
            context.drawText(textRenderer, displayText, fieldX, textY + 1, 0xFF000000, false);

            // 2. Яркий текст поверх
            context.drawText(textRenderer, displayText, fieldX, textY, 0xFFFFFF00, false); // Желтый

            textY += lineHeight;
        }
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static List<String> getCurrentSuggestions() {
        return currentSuggestions;
    }

    public static void loadFromResourcePacks(ResourceManager resourceManager) {
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

                        for (Map.Entry<String, com.google.gson.JsonElement> entry : shortcutsObj.entrySet()) {
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
}
