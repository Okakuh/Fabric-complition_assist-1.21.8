package net.okakuh.complition_assist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplitionAssistClient implements ClientModInitializer {
    public static final String MOD_ID = "complition_assist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<String, String> SHORTCUTS = new HashMap<>();
    private static int tickCount = 0;

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
        LOGGER.info("✅✅✅ COMPLITION ASSIST MOD INITIALIZED ✅✅✅");

        // Загрузка конфигурации
        initializeShortcuts();

        // ВАЖНО: Регистрация HudRenderCallback ДО других обработчиков
        LOGGER.info("Registering HudRenderCallback...");

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
        LOGGER.info("Handlers registered");
    }

    private void initializeShortcuts() {
        // Базовые сокращения
        SHORTCUTS.put("пкд", "Привет как дела?");
        SHORTCUTS.put("гг", "Хорошей игры!");
        SHORTCUTS.put("спс", "Спасибо!");
        SHORTCUTS.put("нг", "С наступающим!");
        SHORTCUTS.put("омг", "О боже мой!");
        SHORTCUTS.put("лол", "😂");
        SHORTCUTS.put("рп", "Ролевая игра");
        SHORTCUTS.put("пж", "Пожалуйста");
        SHORTCUTS.put("нп", "Не за что!");
        SHORTCUTS.put("мб", "Может быть");

        // Английские примеры
        SHORTCUTS.put("gg", "Good game!");
        SHORTCUTS.put("ty", "Thank you!");
        SHORTCUTS.put("np", "No problem!");
        SHORTCUTS.put("brb", "Be right back!");
        SHORTCUTS.put("afk", "Away from keyboard");
        SHORTCUTS.put("test", "Это тестовая замена!");

        LOGGER.info("Loaded {} shortcuts", SHORTCUTS.size());
    }

    private void onClientTick(MinecraftClient client) {
        tickCount++;

        // Обрабатываем обнаруженное двоеточие
        if (colonDetected) {
            colonDetected = false;
            if (!isTracking) {
                startTracking();
                LOGGER.info("🎯 Двоеточие обнаружено! Начинаем отслеживание...");
            }
        }

        // Обрабатываем ожидающую замену
        if (pendingReplacement != null) {
            processReplacement(client);
            pendingReplacement = null;
        }

        // Логируем только каждые 200 тиков
        if (tickCount % 200 == 0) {
            LOGGER.info("Tick #{}", tickCount);
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
        LOGGER.info("Символ введен: '{}' (код: {})", character, (int) character);

        // Проверяем двоеточие
        if (character == ':') {
            // Сбрасываем предыдущую строку и начинаем запись заново
            resetTracking();
            colonDetected = true;
            LOGGER.info("Обнаружено двоеточие! Сбрасываем и начинаем новую последовательность.");
            return;
        }

        // Если ведем отслеживание, добавляем символы
        if (isTracking) {
            // Проверяем максимальную длину (20 символов)
            if (currentSequence.length() >= 20) {
                LOGGER.info("Достигнута максимальная длина последовательности (20 символов). Сбрасываем отслеживание.");
                resetTracking();
                return;
            }

            // Добавляем символ в последовательность
            currentSequence.append(character);
            LOGGER.info("Добавлен символ '{}'. Последовательность: {}", character, currentSequence.toString());
        }
    }

    // Этот метод нужно будет вызывать из Mixin при нажатии специальных клавиш
    // Этот метод нужно будет вызывать из Mixin при нажатии специальных клавиш
    public static void onKeyPressed(int keyCode, int modifiers) {
        LOGGER.info("Клавиша нажата: код {}, модификаторы: {}", keyCode, modifiers);

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
                    LOGGER.info("Достигнута максимальная длина последовательности (20 символов). Сбрасываем отслеживание.");
                    resetTracking();
                    return;
                }

                currentSequence.append(' ');
                LOGGER.info("Пробел добавлен. Последовательность: '{}'", currentSequence.toString());
            }
            return;
        }

        if (!isTracking) return;

        // Проверяем Backspace
        if (keyCode == 259) { // GLFW.GLFW_KEY_BACKSPACE
            if (currentSequence.length() > 0) {
                currentSequence.deleteCharAt(currentSequence.length() - 1);
                LOGGER.info("Backspace. Текущая последовательность: '{}'", currentSequence.toString());
            } else {
                // Backspace на пустой последовательности после двоеточия - сбрасываем
                LOGGER.info("Backspace на пустой последовательности. Сбрасываем отслеживание.");
                resetTracking();
            }
            return;
        }

        // Escape - сбрасываем отслеживание
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            LOGGER.info("Escape нажат. Сбрасываем отслеживание.");
            resetTracking();
            return;
        }
    }

    private static void startTracking() {
        isTracking = true;
        currentSequence = new StringBuilder();
        LOGGER.info("Начато отслеживание последовательности");
    }

    private static void resetTracking() {
        isTracking = false;
        currentSequence = new StringBuilder();
        LOGGER.info("Отслеживание сброшено");
    }

    private static void processTabReplacement() {
        String sequence = currentSequence.toString();
        String replacement = SHORTCUTS.get(sequence.toLowerCase());

        if (replacement != null && !sequence.isEmpty()) {
            LOGGER.info("✅ НАЙДЕНО СОВПАДЕНИЕ! Заменяем '{}' на '{}'", sequence, replacement);

            // Создаем задачу на замену
            pendingReplacement = new ReplacementTask(sequence, replacement);

            // Сбрасываем отслеживание
            resetTracking();
        } else {
            LOGGER.info("❌ Совпадение не найдено для последовательности: '{}'", sequence);
            resetTracking();
        }
    }

    private void processReplacement(MinecraftClient client) {
        if (pendingReplacement == null) return;

        try {
            LOGGER.info("Выполняем замену: удаляем '{}', вставляем '{}'",
                    pendingReplacement.sequence, pendingReplacement.replacement);

            // Симулируем Backspace для удаления двоеточия и последовательности
            simulateBackspaces(client, pendingReplacement.sequence.length() + 2);

            // Вставляем замену
            simulateTextInput(client, pendingReplacement.replacement);

            LOGGER.info("✅ Замена выполнена успешно!");

        } catch (Exception e) {
            LOGGER.error("Ошибка при выполнении замены: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void simulateBackspaces(MinecraftClient client, int count) {
        try {
            // Получаем доступ к приватному методу onKey через reflection
            Method onKeyMethod = Keyboard.class.getDeclaredMethod(
                    "onKey", long.class, int.class, int.class, int.class, int.class
            );
            onKeyMethod.setAccessible(true);

            long window = client.getWindow().getHandle();
            Keyboard keyboard = client.keyboard;

            LOGGER.info("Симулируем {} нажатий Backspace", count);

            for (int i = 0; i < count; i++) {
                // Симулируем нажатие Backspace (PRESS)
                onKeyMethod.invoke(keyboard, window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);

                // УБИРАЕМ задержку
                // try { Thread.sleep(30); } catch (InterruptedException e) {}

                // Симулируем отпускание Backspace (RELEASE)
                onKeyMethod.invoke(keyboard, window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_RELEASE, 0);

                // УБИРАЕМ задержку между нажатиями
                // try { Thread.sleep(30); } catch (InterruptedException e) {}
            }

        } catch (Exception e) {
            LOGGER.error("Ошибка при симуляции Backspace: {}", e.getMessage());
            throw new RuntimeException("Failed to simulate backspace", e);
        }
    }

    private void simulateTextInput(MinecraftClient client, String text) {
        try {
            // Получаем доступ к приватному методу onChar через reflection
            Method onCharMethod = Keyboard.class.getDeclaredMethod(
                    "onChar", long.class, int.class, int.class
            );
            onCharMethod.setAccessible(true);

            long window = client.getWindow().getHandle();
            Keyboard keyboard = client.keyboard;

            LOGGER.info("Симулируем ввод текста: '{}'", text);

            for (char c : text.toCharArray()) {
                onCharMethod.invoke(keyboard, window, (int)c, 0);

                // УБИРАЕМ задержку между символами
                // try { Thread.sleep(30); } catch (InterruptedException e) {}
            }

        } catch (Exception e) {
            LOGGER.error("Ошибка при симуляции ввода текста: {}", e.getMessage());
            throw new RuntimeException("Failed to simulate text input", e);
        }
    }

    public static void addShortcut(String shortcut, String replacement) {
        SHORTCUTS.put(shortcut.toLowerCase(), replacement);
        LOGGER.info("Добавлено сокращение: {} -> {}", shortcut, replacement);
    }

    public static Map<String, String> getShortcuts() {
        return new HashMap<>(SHORTCUTS);
    }

    private static List<String> getSuggestions(String input) {
        LOGGER.info("Getting suggestions for input: '{}'", input);

        List<String> suggestions = new ArrayList<>();
        String inputLower = input.toLowerCase();

        for (String shortcut : SHORTCUTS.keySet()) {
            if (shortcut.toLowerCase().startsWith(inputLower)) {
                suggestions.add(shortcut);
            }
        }

        suggestions.sort(String::compareToIgnoreCase);
        if (suggestions.size() > 5) {
            suggestions = suggestions.subList(0, 5);
        }

        LOGGER.info("Found {} suggestions: {}", suggestions.size(), suggestions);
        return suggestions;
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

        // ТЕСТ: нарисовать большой красный текст в центре экрана
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        context.drawText(textRenderer, "ТЕСТ ТЕКСТА",
                screenWidth/2 - 50, screenHeight/2, 0xFF0000, true);

        // Остальной код...

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

            // 3. Добавляем лог
            ComplitionAssistClient.LOGGER.info("🎨 Рисую: '{}' на {},{}", displayText, fieldX, textY);

            textY += lineHeight;
        }
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static List<String> getCurrentSuggestions() {
        return currentSuggestions;
    }

}