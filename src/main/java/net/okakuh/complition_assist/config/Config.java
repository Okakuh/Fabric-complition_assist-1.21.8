package net.okakuh.complition_assist.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.okakuh.complition_assist.ComplitionAssist;

import java.io.*;
import java.nio.file.*;

public class Config {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("yourmod.json");
    private static Config INSTANCE;

    // Единственная настройка - включение/выключение мода
    private boolean modEnabled = true;

    public static Config getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Config();
        }
        return INSTANCE;
    }

    private Config() {
        load();
    }

    public void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                JsonObject json = JsonParser.parseReader(Files.newBufferedReader(CONFIG_PATH)).getAsJsonObject();

                if (json.has("modEnabled")) {
                    modEnabled = json.get("modEnabled").getAsBoolean();
                }
            } else {
                save(); // Создать файл с настройками по умолчанию
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            save();
        }
    }

    public void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("modEnabled", modEnabled);

            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    // Геттер и сеттер
    public boolean isModEnabled() {
        return modEnabled;
    }

    public void setModEnabled(boolean enabled) {
        this.modEnabled = enabled;
        save();
        ComplitionAssist.setWorking(enabled);
    }

    // Для быстрого переключения
    public void toggleModEnabled() {
        setModEnabled(!modEnabled);
    }
}