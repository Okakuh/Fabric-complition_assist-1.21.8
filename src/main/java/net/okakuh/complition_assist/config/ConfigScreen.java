package net.okakuh.complition_assist.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.complition_assist.config"))
                .setSavingRunnable(Config.getInstance()::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Основная категория
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.complition_assist.general"));

        // Добавляем единственную настройку - кнопку включения/выключения
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("option.complition_assist.mod_enabled"),
                        Config.getInstance().isModEnabled()
                )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.complition_assist.mod_enabled"))
                .setSaveConsumer(Config.getInstance()::setModEnabled)
                .build());

        return builder.build();
    }
}