package net.okakuh.complition_assist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.EditBox;
import net.okakuh.complition_assist.mixin.KeyboardAccessor;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class Util {
    public static EditBox asdf = null;
    public static void processReplacement(MinecraftClient client, String sequence, String replacement) {
        // Симулируем Backspace для удаления двоеточия и последовательности
//        simulateBackspaces(client, sequence.length() + 1);

        // Вставляем замену
//        simulateTextInput(client, "§" + "7" + replacement);
        if (asdf != null) {
            ComplitionAssist.LOGGER.info("asdf");
            String text = asdf.getText();
            CharSequence b = (CharSequence) (replacement);
            String d = text.replace((CharSequence) sequence, b);
            asdf.setText(d);
        }
    }

    public static void simulateBackspaces(MinecraftClient client, int count) {
        KeyboardAccessor keyboard = (KeyboardAccessor) client.keyboard;
        long window = client.getWindow().getHandle();

        for (int i = 0; i < count; i++) {
            keyboard.invokeOnKey(window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);
            keyboard.invokeOnKey(window, GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_RELEASE, 0);
        }
    }

    public static void simulateTextInput(MinecraftClient client, String text) {
        KeyboardAccessor keyboard = (KeyboardAccessor) client.keyboard;
        long window = client.getWindow().getHandle();
        for (char c : text.toCharArray()) {
            keyboard.invokeOnChar(window, (int) c, 0);
        }
    }

    public static List<String> parseSuggestions(String sequence, Map<String, String> shortcuts) {
        Set<String> startsWith = new LinkedHashSet<>();
        Set<String> containsOnly = new LinkedHashSet<>();

        String inputLower = sequence.toLowerCase();

        // Один проход по всем сокращениям
        for (String shortcut : shortcuts.keySet()) {
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

        // Сортируем первую группу (startsWith)
        List<String> sortedStartsWith = new ArrayList<>(startsWith);
        sortedStartsWith.sort(String::compareToIgnoreCase);
        List<String> sortedResult = new ArrayList<>(sortedStartsWith);

        // Сортируем вторую группу (containsOnly)
        List<String> sortedContainsOnly = new ArrayList<>(containsOnly);
        sortedContainsOnly.sort(String::compareToIgnoreCase);
        sortedResult.addAll(sortedContainsOnly);

        // Ограничиваем количество
        if (sortedResult.size() > 10) return sortedResult.subList(0, 10);

        return sortedResult;

    }

    public static List<String> parseDisplaySuggestions(List<String> suggestions, Map<String, String> shortcuts) {
        List<String> displayList = new ArrayList<>();

        for (String suggestion : suggestions) {
            String fullText = shortcuts.get(suggestion);
            if (fullText != null) {
                String displayText = suggestion + " → " + fullText;
                displayList.add(displayText);
            }
        }
        return displayList;
    }

    public static String parseSequence(String text, String keyChar) {
        int colonIndex = text.lastIndexOf(keyChar);
        if (colonIndex == -1) return "";

        String betweenColonAndEnd = text.substring(colonIndex + 1);

        if (betweenColonAndEnd.contains(" ") || betweenColonAndEnd.contains("\n")) return "";

        return betweenColonAndEnd.toLowerCase();
    }
}
