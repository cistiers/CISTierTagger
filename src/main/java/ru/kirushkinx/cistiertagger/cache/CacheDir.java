package ru.kirushkinx.cistiertagger.cache;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;

import java.nio.file.Path;

@UtilityClass
public class CacheDir {

    public static @NotNull Path root() {
        return FabricLoader.getInstance().getGameDir().resolve("cache").resolve(CisTierTagger.MOD_ID);
    }

    public static @NotNull Path bodies() {
        return root().resolve("bodies");
    }

    public static @NotNull Path heads() {
        return root().resolve("heads");
    }
}
