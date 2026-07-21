package ru.kirushkinx.cistiertagger.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.PlayerInfo;
import ru.kirushkinx.cistiertagger.gui.screen.ProfileScreen;
import ru.kirushkinx.cistiertagger.gui.screen.SearchScreen;
import ru.kirushkinx.cistiertagger.util.Nickname;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

@UtilityClass
public class CisTierCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("cistier")
                    .executes(ctx -> {
                        mc.schedule(() -> mc.setScreen(new SearchScreen()));
                        return 1;
                    })
                    .then(ClientCommands.argument("nickname", StringArgumentType.word())
                            .suggests(ONLINE_PLAYERS)
                            .executes(ctx -> {
                                String nick = StringArgumentType.getString(ctx, "nickname");
                                mc.schedule(() -> ProfileScreen.openFor(nick));
                                return 1;
                            })));
        });
    }

    private static final SuggestionProvider<FabricClientCommandSource> ONLINE_PLAYERS = (ctx, builder) -> {
        if (mc.getConnection() != null) {
            String remaining = Nickname.normalize(builder.getRemaining());
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                if (name == null) continue;
                if (Nickname.normalize(name).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
        }
        return builder.buildFuture();
    };
}
