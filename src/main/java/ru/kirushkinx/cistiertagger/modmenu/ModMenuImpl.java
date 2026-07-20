package ru.kirushkinx.cistiertagger.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.gui.screen.ConfigScreen;

public class ModMenuImpl implements ModMenuApi {

    @Override
    public @NotNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }
}
