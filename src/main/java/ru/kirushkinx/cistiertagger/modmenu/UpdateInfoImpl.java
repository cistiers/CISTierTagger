package ru.kirushkinx.cistiertagger.modmenu;

import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.api.UpdateChecker;

public class UpdateInfoImpl implements UpdateInfo {

    @Override
    public boolean isUpdateAvailable() {
        return current() != null;
    }

    @Override
    public @Nullable String getDownloadLink() {
        UpdateChecker.Update update = current();
        return update == null ? null : update.url();
    }

    @Override
    public Component getUpdateMessage() {
        UpdateChecker.Update update = current();
        return update == null ? null : Component.translatable("cistiertagger.update.toast.description", update.version());
    }

    @Override
    public UpdateChannel getUpdateChannel() {
        return UpdateChannel.RELEASE;
    }

    private static UpdateChecker.@Nullable Update current() {
        UpdateChecker checker = CisTierTagger.getUpdateChecker();
        return checker == null ? null : checker.available();
    }
}
