[English](restriction.md) | Русский

## Ограничение со стороны сервера
Сервер может сообщить клиенту CISTierTagger, чтобы скрыть бейджи - все сразу или по отдельности.

При входе клиент сам сообщает о себе, отправляя пустое сообщение `cistiertagger:handshake`; сервер, который хочет его ограничить, отвечает по `cistiertagger:restrict`. Это opt-in: сервер, игнорирующий handshake, оставляет мод работать как обычно, поэтому ванильные серверы не затрагиваются. Ставить на сервер ничего не нужно, кроме вашего собственного плагина.

<p align="left"><img src="assets/restriction_message.png"></p>

### Пакеты

- **`cistiertagger:handshake`** - serverbound (клиент -> сервер), пустой. Клиент отправляет его сразу после входа; получение означает, что у игрока есть мод. Это надежный способ детекта мода (пакеты регистрации каналов уходят в configuration-фазу и их легко пропустить).
- **`cistiertagger:restrict`** - clientbound (сервер -> клиент). Три булина, по одному байту (`0` = показать, `1` = скрыть), по порядку: nametag, tab, chat.

### Поведение

- Ответь на handshake пакетом restrict. Пересылай в любой момент, чтобы изменить или снять ограничение.
- Клиент сам снимает ограничение при выходе, поэтому оно действует только на сервере, который его установил.
- При получении ограничивающего пакета клиент перестает рисовать указанные поверхности и пишет одну строку в чат, в hover которой перечислены отключенные поверхности.

### Примеры реализации

Жди от клиента `cistiertagger:handshake` и отвечай по `cistiertagger:restrict`. Порядок байтов всегда `nametag, tab, chat`; `{ 1, 1, 1 }` скрывает все, `{ 0, 0, 1 }` только чат, `{ 0, 0, 0 }` снимает ограничение.

#### Bukkit
```java
// onEnable()
getServer().getMessenger().registerOutgoingPluginChannel(this, "cistiertagger:restrict");
getServer().getMessenger().registerIncomingPluginChannel(this, "cistiertagger:handshake", new CisTierRestrictor());
```

```java
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import static your.plugin.Main.plugin;

public class CisTierRestrictor implements PluginMessageListener {

    private static final String RESTRICT = "cistiertagger:restrict";

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        byte[] payload = { 1, 1, 1 }; // nametag, tab, chat
        player.sendPluginMessage(plugin, RESTRICT, payload);
    }
}
```

#### PacketEvents

```java
// onEnable()
PacketEvents.getAPI().getEventManager().registerListener(new CisTierRestrictor(), PacketListenerPriority.NORMAL);
```

```java
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;

public class CisTierRestrictor implements PacketListener {

    private static final String HANDSHAKE = "cistiertagger:handshake";
    private static final String RESTRICT = "cistiertagger:restrict";

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        WrapperPlayClientPluginMessage message = new WrapperPlayClientPluginMessage(event);
        if (!message.getChannelName().equals(HANDSHAKE)) return;

        byte[] payload = { 1, 1, 1 }; // nametag, tab, chat
        event.getUser().sendPacket(new WrapperPlayServerPluginMessage(RESTRICT, payload));
    }
}
```
