package net.okakuh.complition_assist;

import com.google.gson.JsonElement;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceType;
import net.okakuh.complition_assist.suggesters.RenderData;
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
import java.util.function.Consumer;

public class ComplitionAssist implements ClientModInitializer {
    public static final String MOD_ID = "complition_assist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier SHORTCUTS_ID = Identifier.of("complition_assist", "suggestions.json");

    private static Consumer<Integer> replaceConsumer;
    private static RenderData renderData;
    public static boolean suggesting = false;

    private static final Map<String, String> suggestionsALL = new HashMap<>();

    private static boolean WORKING = true;
    private static final String keyChar = ":";

    private static final int borderPadding = 2;
    private static final int borderWidth = 1;
    private static final int lineHeight = 10;

    private static int SUGGESTIONS_X = 0;
    private static int SUGGESTIONS_Y = 0;
    private static int SUGGESTIONS_WIDTH = 0;
    private static int SUGGESTIONS_HEIGHT = 0;

    private static final int borderColor = 0xFFaba9a2;
    private static final int textColor = 0xFFFFFFFF;
    private static final int backgroundColor = 0xB3000000;

    private static boolean isDisplaySuggestionsMirrored = false;

    @Override
    public void onInitializeClient() {
        // Регистрация ResourceReloadListener
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("complition_assist", "suggestions_loader");
                    }

                    @Override
                    public void reload(ResourceManager resourceManager) {
                        loadFromResourcePacks(resourceManager);
                    }
                }
        );
    }

    public static void onKeyPressed(int keyCode, int modifiers) {
        boolean shiftPressed = (modifiers & 1) != 0;
        boolean spacePressed = keyCode == 32;


        if (suggesting && WORKING) {
            if (shiftPressed && spacePressed) {
                replaceConsumer.accept(0);
            }
        }
    }

    private static void loadFromResourcePacks(ResourceManager resourceManager) {
        suggestionsALL.clear();

        // Загружаем из всех ресурспаков
        try {
            List<Resource> resources = resourceManager.getAllResources(SHORTCUTS_ID);

            for (Resource resource : resources) {
                int i = 0;

                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);

                    if (json.has("suggestions")) {
                        JsonObject shortcutsObj = json.getAsJsonObject("suggestions");

                        for (Map.Entry<String, JsonElement> entry : shortcutsObj.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue().getAsString();
                            suggestionsALL.put(key, value);
                            i ++;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading shortcuts from {}", resource.getPack(), e);
                }
                if (i > 0) {
                    LOGGER.info("Loaded {} suggestions from resource pack: {}", i, resource.getPack().getId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading shortcuts", e);
        }
    }

    public static boolean isNotWorking() {return !WORKING ;}

    public static void setWorking(boolean working) {WORKING = working;}

    public static String getKeyChar() {
        return keyChar;
    }

    public static Map<String, String> getSuggestions() {
        return suggestionsALL;
    }

    public static int getLineHeight() {
        return lineHeight;
    }

    public static void setNewRenderData(RenderData renderDat) {
        renderData = renderDat;
        // Координата Х для рендера подсказок
        SUGGESTIONS_X = renderData.textCursorX;
        // Координата У для рендера подсказок
        SUGGESTIONS_Y = renderData.textCursorY;

        // Определение высоты и ширины с фоном и границей
        // Высота
        int suggestionCount = renderData.displaySuggestionsForSequence.size();
        int suggestionsHeight = suggestionCount * lineHeight;

        // Ширина
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;
        int maxWidth = 0;
        for (String displayText : renderData.displaySuggestionsForSequence) {
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(displayText));
        }

        SUGGESTIONS_HEIGHT = suggestionsHeight + (borderPadding * 2) + (borderWidth * 2);
        SUGGESTIONS_WIDTH = maxWidth + (borderPadding * 2) + (borderWidth * 2);

        // Сдвиг по Х для рендера подсказок красиво символ в символ под текстом
        SUGGESTIONS_X -= client.textRenderer.getWidth(renderData.sequence);

        // Добавляем защиту от рендера вне экрана

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // По высоте
        if ((screenHeight - (SUGGESTIONS_Y + renderData.suggestions_Y_offset)) > SUGGESTIONS_HEIGHT) {
            SUGGESTIONS_Y += renderData.suggestions_Y_offset;
            isDisplaySuggestionsMirrored = false;

        } else {
            isDisplaySuggestionsMirrored = true;
            SUGGESTIONS_Y -= renderData.suggestions_Y_offset + SUGGESTIONS_HEIGHT;

            // Отзеркаливаем список если будем рендерить подсказки сверху
            // Чтобы первое предложение на замену было снизу
            renderData.displaySuggestionsForSequence = renderData.displaySuggestionsForSequence.reversed();
        }
        // По ширине
        if (SUGGESTIONS_X + SUGGESTIONS_WIDTH > screenWidth) SUGGESTIONS_X -= SUGGESTIONS_WIDTH;
    }

    public static void setNewReplacer(Consumer<Integer> replace_consumer) {
        replaceConsumer = replace_consumer;
    }

    public static void setSuggesting(boolean suggest) {
        suggesting = suggest;
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

        String inRowSuggestion = renderData.suggestionsForSequence.getFirst();
        String stripedInRowSuggestion = inRowSuggestion.substring(renderData.sequence.length());

        context.drawText(client.textRenderer, stripedInRowSuggestion,
                renderData.textCursorX + 3, renderData.suggestionInRowY,
                renderData.inRowSuggestionColor, false);

        // Рендер текста
        for (String displayText : renderData.displaySuggestionsForSequence) {
            context.drawText(client.textRenderer, displayText, textX, textY, textColor, true);

            textY += lineHeight;
        }
    }

    public static void onMouseClick(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int nonText = borderWidth + borderPadding;
        int X1 = SUGGESTIONS_X + nonText;
        int Y1 = SUGGESTIONS_Y + nonText;
        int X2 = SUGGESTIONS_X + SUGGESTIONS_WIDTH - nonText;
        int Y2 = SUGGESTIONS_Y + SUGGESTIONS_HEIGHT - nonText;

        if (X1 > mouseX || mouseX > X2) return;
        if (Y1 > mouseY || mouseY > Y2) return;

        float OffsetFromBottom = (float) (Y2 - mouseY) / lineHeight;

        int index = (int) Math.floor(OffsetFromBottom);
        if (!isDisplaySuggestionsMirrored)
            index = renderData.suggestionsForSequence.size() - (index + 1);

        replaceConsumer.accept(index);
    }

    public static void tryRender(DrawContext context) {
        if (!WORKING) return;
        if (!suggesting) return;
        render(context);
    }
}