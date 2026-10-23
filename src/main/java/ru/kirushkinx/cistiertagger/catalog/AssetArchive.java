package ru.kirushkinx.cistiertagger.catalog;

import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import ru.kirushkinx.cistiertagger.util.Json;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@UtilityClass
public class AssetArchive {

    public static final int MAX_ARCHIVE = 32 * 1024 * 1024;
    private static final int MAX_UNPACKED = 128 * 1024 * 1024;

    public static void verify(byte[] bytes, TierCatalog.Archive archive) throws Exception {
        if (archive == null || archive.size() <= 0 || archive.size() > MAX_ARCHIVE
                || bytes.length != archive.size() || !archive.sha256().matches("[a-f0-9]{64}")
                || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(archive.sha256())) {
            throw new IOException("Asset archive checksum or size mismatch");
        }
    }

    public static Map<String, byte[]> validate(byte[] bytes, TierCatalog catalog) throws Exception {
        verify(bytes, catalog.archive());
        Map<String, Integer> directory = checkCentralDirectory(bytes); // match both zip readers
        Map<String, byte[]> files = new HashMap<>();
        int total = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String path = entry.getName();
                if (entry.isDirectory() || !allowed(path) || files.containsKey(path) || files.size() >= 6402) {
                    throw new IOException("Unexpected asset path: " + path);
                }
                byte[] data = zip.readNBytes(8 * 1024 * 1024 + 1);
                total += data.length;
                if (data.length > 8 * 1024 * 1024 || total > MAX_UNPACKED) throw new IOException("Asset pack too large");
                Integer offset = directory.remove(path);
                if (offset == null) throw new IOException("ZIP entry missing from directory");
                ByteBuffer central = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                if (entry.getCrc() != Integer.toUnsignedLong(central.getInt(offset + 16))
                        || entry.getCompressedSize() != Integer.toUnsignedLong(central.getInt(offset + 20))
                        || entry.getSize() != Integer.toUnsignedLong(central.getInt(offset + 24))) {
                    throw new IOException("ZIP directory does not match entry data");
                }
                if (path.endsWith(".png")) validatePng(data);
                files.put(path, data);
            }
        }
        if (!directory.isEmpty()) throw new IOException("ZIP directory contains missing entries");
        validateFonts(files, catalog);
        return files;
    }

    public static void validate(Path archive, TierCatalog catalog) throws Exception {
        try (var input = Files.newInputStream(archive)) {
            validate(input.readNBytes(MAX_ARCHIVE + 1), catalog);
        }
    }

    private static boolean allowed(String path) {
        return path.matches("assets/cistiertagger/font/(icons|badges)\\.json")
                || path.matches("assets/cistiertagger/textures/icons/[a-z0-9][a-z0-9_]{0,63}\\.png")
                || path.matches("assets/cistiertagger/textures/badges/[a-z0-9][a-z0-9_]{0,63}/[a-z0-9][a-z0-9_]{0,31}\\.png");
    }

    private static void validatePng(byte[] bytes) throws IOException {
        if (bytes.length < 33 || ByteBuffer.wrap(bytes).getLong() != 0x89504e470d0a1a0aL) throw new IOException("Invalid PNG");
        ByteBuffer header = ByteBuffer.wrap(bytes);
        int width = header.getInt(16), height = header.getInt(20);
        if (width < 1 || height < 1 || width > 2048 || height > 2048) throw new IOException("Invalid texture dimensions");
        if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) throw new IOException("Unreadable PNG");
    }

    private static void validateFonts(Map<String, byte[]> files, TierCatalog catalog) throws IOException {
        for (String font : new String[]{"icons", "badges"}) {
            byte[] data = files.get("assets/cistiertagger/font/" + font + ".json");
            if (data == null) throw new IOException("Missing font: " + font);
            JsonObject json = Json.GSON.fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
            Map<Integer, String> glyphs = new HashMap<>();
            for (var element : json.getAsJsonArray("providers")) {
                var provider = element.getAsJsonObject();
                if (!"bitmap".equals(provider.get("type").getAsString())) throw new IOException("Unexpected font provider");
                String resource = provider.get("file").getAsString();
                if (!resource.startsWith("cistiertagger:")) throw new IOException("Unexpected font namespace");
                String path = "assets/cistiertagger/textures/" + resource.substring("cistiertagger:".length());
                var rows = provider.getAsJsonArray("chars");
                String chars = rows.size() == 1 ? rows.get(0).getAsString() : "";
                if (!allowed(path) || !files.containsKey(path) || chars.codePointCount(0, chars.length()) != 1
                        || provider.get("height").getAsInt() != (font.equals("icons") ? 12 : 8)
                        || provider.get("ascent").getAsInt() != (font.equals("icons") ? 10 : 7)
                        || glyphs.put(chars.codePointAt(0), path) != null) throw new IOException("Invalid bitmap glyph");
            }
            for (var mode : catalog.tierlists().values()) {
                var expected = font.equals("icons") ? List.of(mode.icon()) : mode.badges().values();
                for (var glyph : expected) {
                    if (!glyph.path().equals(glyphs.get(glyph.codepoint()))) throw new IOException("Catalog/font mismatch");
                }
            }
        }
    }

    private static Map<String, Integer> checkCentralDirectory(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int end = -1;
        for (int i = bytes.length - 22; i >= Math.max(0, bytes.length - 65557); i--) {
            if (buffer.getInt(i) == 0x06054b50 && i + 22 + Short.toUnsignedInt(buffer.getShort(i + 20)) == bytes.length) {
                end = i;
                break;
            }
        }
        if (end < 0 || buffer.getShort(end + 4) != 0 || buffer.getShort(end + 6) != 0) throw new IOException("Invalid ZIP directory");
        int count = Short.toUnsignedInt(buffer.getShort(end + 10));
        int offset = buffer.getInt(end + 16);
        if (count > 6402 || count == 0 || offset < 0) throw new IOException("Invalid ZIP limits");
        Map<String, Integer> names = new HashMap<>();
        for (int i = 0; i < count; i++) {
            if (offset < 0 || offset + 46 > end || buffer.getInt(offset) != 0x02014b50) throw new IOException("Invalid ZIP entry");
            int length = Short.toUnsignedInt(buffer.getShort(offset + 28));
            int extra = Short.toUnsignedInt(buffer.getShort(offset + 30));
            int comment = Short.toUnsignedInt(buffer.getShort(offset + 32));
            int mode = buffer.getInt(offset + 38) >>> 16; // unix file type
            if ((mode & 0170000) == 0120000 || (buffer.getShort(offset + 8) & 1) != 0
                    || offset + 46 + length + extra + comment > end) throw new IOException("Unsupported ZIP entry");
            String path = new String(bytes, offset + 46, length, StandardCharsets.UTF_8);
            if (!allowed(path) || names.put(path, offset) != null) throw new IOException("Invalid or duplicate ZIP path");
            int local = buffer.getInt(offset + 42);
            if (local < 0 || local + 30 > end || buffer.getInt(local) != 0x04034b50
                    || buffer.getShort(local + 6) != buffer.getShort(offset + 8)
                    || buffer.getShort(local + 8) != buffer.getShort(offset + 10)) throw new IOException("Invalid local ZIP entry");
            int localLength = Short.toUnsignedInt(buffer.getShort(local + 26));
            if (local + 30 + localLength > end || !path.equals(new String(bytes, local + 30, localLength, StandardCharsets.UTF_8))) {
                throw new IOException("ZIP directory path does not match local entry");
            }
            offset += 46 + length + extra + comment;
        }
        if (offset != end) throw new IOException("Invalid ZIP directory size");
        return names;
    }
}
