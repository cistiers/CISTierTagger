package ru.kirushkinx.cistiertagger.network;

import lombok.Getter;
import lombok.experimental.UtilityClass;

/** Per-connection display restrictions pushed by the server. */
@UtilityClass
public class ServerRestrictions {

    @Getter
    private volatile boolean nametagRestricted;
    @Getter
    private volatile boolean tabRestricted;
    @Getter
    private volatile boolean chatRestricted;

    public void apply(boolean nametag, boolean tab, boolean chat) {
        nametagRestricted = nametag;
        tabRestricted = tab;
        chatRestricted = chat;
    }

    public void clear() {
        apply(false, false, false);
    }

    public boolean isAnyRestricted() {
        return nametagRestricted || tabRestricted || chatRestricted;
    }
}
