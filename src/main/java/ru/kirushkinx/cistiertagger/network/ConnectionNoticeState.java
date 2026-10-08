package ru.kirushkinx.cistiertagger.network;

/** A play listener can be replaced during reconfiguration; the underlying connection survives. */
final class ConnectionNoticeState<T> {
    private T notifiedConnection;

    boolean shouldNotify(T connection) {
        if (connection == notifiedConnection) return false;
        notifiedConnection = connection;
        return true;
    }
}
