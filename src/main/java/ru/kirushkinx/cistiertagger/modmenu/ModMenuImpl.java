package ru.kirushkinx.cistiertagger.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//? if >=1.20.5 {
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;
//?}
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.gui.screen.ConfigScreen;

public class ModMenuImpl implements ModMenuApi {

//? if >=1.20.5 {
    private static final UpdateInfo UPDATE_INFO = new UpdateInfoImpl();

//?}
    @Override
    public @NotNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }
//? if >=1.20.5 {

    @Override
    public UpdateChecker getUpdateChecker() {
        return () -> UPDATE_INFO;
    }
//?}
}
