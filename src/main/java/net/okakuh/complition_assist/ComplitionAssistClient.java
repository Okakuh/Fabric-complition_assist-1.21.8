package net.okakuh.complition_assist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ComplitionAssistClient implements ClientModInitializer {
    public static final String MOD_ID = "complition_assist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<String, String> SHORTCUTS = new HashMap<>();
    private static int tickCount = 0;

    // Состояние отслеживания
    private static boolean isTracking = false;
    private static StringBuilder currentSequence = new StringBuilder();

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

        initializeShortcuts();

        // Регистрация обработчика тиков
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

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
            LOGGER.debug("Tick #{}", tickCount);
        }
    }

    // Этот метод нужно будет вызывать из Mixin при вводе символов
    public static void onCharTyped(char character) {
        LOGGER.debug("Символ введен: '{}' (код: {})", character, (int) character);

        // Проверяем двоеточие
        if (character == ':') {
            // Сбрасываем предыдущую строку и начинаем запись заново
            resetTracking();
            colonDetected = true;
            LOGGER.debug("Обнаружено двоеточие! Сбрасываем и начинаем новую последовательность.");
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
        LOGGER.debug("Клавиша нажата: код {}, модификаторы: {}", keyCode, modifiers);

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

    // API методы
    public static void addShortcut(String shortcut, String replacement) {
        SHORTCUTS.put(shortcut.toLowerCase(), replacement);
        LOGGER.info("Добавлено сокращение: {} -> {}", shortcut, replacement);
    }

    public static void removeShortcut(String shortcut) {
        SHORTCUTS.remove(shortcut.toLowerCase());
        LOGGER.info("Удалено сокращение: {}", shortcut);
    }

    public static Map<String, String> getShortcuts() {
        return new HashMap<>(SHORTCUTS);
    }
}