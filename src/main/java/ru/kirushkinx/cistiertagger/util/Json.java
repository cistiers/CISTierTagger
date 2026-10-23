package ru.kirushkinx.cistiertagger.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.model.Gamemode;

import java.io.IOException;

@UtilityClass
public class Json {

    public static final @NotNull Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Gamemode.class, new GamemodeAdapter())
            .disableHtmlEscaping()
            .create();

    public static final @NotNull Gson PRETTY = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Gamemode.class, new GamemodeAdapter())
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static final class GamemodeAdapter extends TypeAdapter<Gamemode> {
        @Override
        public void write(JsonWriter out, Gamemode value) throws IOException {
            if (value == null) out.nullValue();
            else out.value(value.apiKey());
        }

        @Override
        public Gamemode read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return Gamemode.fromApiKey(in.nextString()).orElse(null);
        }
    }
}
