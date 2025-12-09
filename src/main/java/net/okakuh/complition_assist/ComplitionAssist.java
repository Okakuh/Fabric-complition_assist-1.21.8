package net.okakuh.complition_assist;

import com.google.gson.JsonElement;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
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

import static net.okakuh.complition_assist.Util.*;

public class ComplitionAssist implements ClientModInitializer {
    public static final String MOD_ID = "complition_assist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier SHORTCUTS_ID = Identifier.of("complition_assist", "suggestions.json");

    private static boolean WORKING = true;

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


        if (Suggestions.isON() && WORKING) {
            if (shiftPressed && spacePressed) {
                String sequence = Suggestions.getSequence();
                String replacement = Suggestions.getReplacement();
                if (replacement != null) {
                    processReplacement(MinecraftClient.getInstance(), sequence, replacement);
                }
            }
        }
    }

    private static void loadFromResourcePacks(ResourceManager resourceManager) {
        Suggestions.OFF();
        Suggestions.clear();

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
                            Suggestions.add(key.toLowerCase(), value);
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

    public static boolean isNotWorking() {
        return !WORKING;
    }

    public static void setWorking(boolean working) {
        WORKING = working;
        if (!WORKING) Suggestions.OFF();
    }
}