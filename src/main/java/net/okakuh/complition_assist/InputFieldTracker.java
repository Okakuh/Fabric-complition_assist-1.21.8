package net.okakuh.complition_assist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.joml.Vector2i;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class InputFieldTracker {
    private static TextFieldWidget activeTextField = null;
    private static Vector2i fieldPosition = new Vector2i(0, 0);

    public static void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            activeTextField = null;
            return;
        }

        activeTextField = findFocusedTextFieldRecursive(client.currentScreen);

        if (activeTextField != null) {
            fieldPosition.x = activeTextField.getX();
            fieldPosition.y = activeTextField.getY(); // Верхняя граница!
        }
    }

    public static TextFieldWidget findFocusedTextFieldRecursive(Screen screen) {
        try {
            // Получаем все children экрана
            Field childrenField = Screen.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            List<ClickableWidget> children = (List<ClickableWidget>) childrenField.get(screen);

            for (ClickableWidget widget : children) {
                if (widget instanceof TextFieldWidget && widget.isFocused()) {
                    return (TextFieldWidget) widget;
                }
            }

            // Если не нашли, пробуем через reflect
            return findTextFieldViaReflection(screen);

        } catch (Exception e) {
            return findTextFieldViaReflection(screen);
        }
    }

    private static TextFieldWidget findTextFieldViaReflection(Screen screen) {
        // Ищем все поля типа TextFieldWidget в классе экрана
        for (Field field : screen.getClass().getDeclaredFields()) {
            if (TextFieldWidget.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    TextFieldWidget textField = (TextFieldWidget) field.get(screen);
                    if (textField != null && textField.isFocused()) {
                        return textField;
                    }
                } catch (Exception e) {
                    // Игнорируем
                }
            }
        }
        return null;
    }

    public static TextFieldWidget getActiveField() {
        return activeTextField;
    }

    public static Vector2i getFieldPosition() {
        return fieldPosition;
    }

    public static boolean hasActiveField() {
        return activeTextField != null;
    }
}