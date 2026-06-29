English | [Русский](restriction_ru.md)

## Server-side restriction
A server can tell the CISTierTagger client to hide its badges - all of them, or one surface at a time.

The client announces itself on join by sending an empty `cistiertagger:handshake` message; a server that wants to restrict it replies on `cistiertagger:restrict`. It is opt-in: a server that ignores the handshake leaves the mod working normally, so vanilla servers are unaffected. Nothing needs to be installed server-side beyond your own plugin.

<p align="left"><img src="assets/restriction_message.png"></p>

### Packets

- **`cistiertagger:handshake`** - serverbound (client -> server), empty. The client sends it once it has joined; receiving it means that player runs the mod. This is the reliable way to detect the mod (channel-registration packets land in the configuration phase and are easy to miss).
- **`cistiertagger:restrict`** - clientbound (server -> client). Three booleans, one byte each (`0` = show, `1` = hide), in order: nametag, tab, chat.

### Behaviour

- Reply to the handshake with a restrict packet. Resend any time to change or lift the restriction.
- The client clears the restriction by itself on disconnect, so it only ever applies to the server that set it.
- On a restricting packet the client stops drawing the listed surfaces and prints a one-line chat notice whose hover lists which surfaces were disabled.

### Implementation examples

Wait for the client's `cistiertagger:handshake`, then reply on `cistiertagger:restrict`. The byte order is always `nametag, tab, chat`; `{ 1, 1, 1 }` hides everything, `{ 0, 0, 1 }` only chat, `{ 0, 0, 0 }` lifts it.

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
