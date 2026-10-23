package ru.kirushkinx.cistiertagger.config;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.model.Gamemode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ModConfig {

    public enum Position {
        LEFT, RIGHT
    }

    public enum DisplayMode {
        PRIORITY, HIGHEST
    }

    public enum BadgeMode {
        IMAGE, TEXT
    }

    private boolean enabled = true;

    private @NotNull Position position = Position.LEFT;

    private @NotNull DisplayMode displayMode = DisplayMode.PRIORITY;

    private @NotNull BadgeMode badgeMode = BadgeMode.IMAGE;

    private @NotNull List<Gamemode> priorityOrder = new ArrayList<>(Arrays.asList(Gamemode.values()));

    private @NotNull Set<Gamemode> enabledGamemodes = new LinkedHashSet<>(Arrays.asList(Gamemode.values()));

    private boolean showInNametag = true;

    private boolean showInTabList = false;

    private boolean showInChat = false;

    private boolean checkForUpdates = true;

    public boolean isGamemodeEnabled(@NotNull Gamemode gamemode) {
        return enabledGamemodes.contains(gamemode);
    }
}
