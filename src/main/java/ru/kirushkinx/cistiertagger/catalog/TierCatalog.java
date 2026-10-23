package ru.kirushkinx.cistiertagger.catalog;

import ru.kirushkinx.cistiertagger.util.Json;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TierCatalog(int schemaVersion, String version, Archive archive, List<String> tiers,
                          Map<String, Mode> tierlists) {

    public record Archive(String path, String sha256, long size) {}

    public record Glyph(String path, int codepoint) {
        public String text() {
            return new String(Character.toChars(codepoint));
        }
    }

    public record Mode(String displayName, String color, String textIcon, boolean isActive,
                       Glyph icon, Map<String, Glyph> badges) {}

    public TierCatalog inOrder(List<String> order) {
        Map<String, Mode> sorted = new LinkedHashMap<>();
        for (String id : order) {
            if (tierlists.containsKey(id)) sorted.put(id, tierlists.get(id));
        }
        tierlists.forEach(sorted::putIfAbsent);
        return new TierCatalog(schemaVersion, version, archive, tiers, Collections.unmodifiableMap(sorted));
    }

    public static TierCatalog parse(byte[] bytes) {
        TierCatalog value = Json.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), TierCatalog.class);
        value.validate();
        return new TierCatalog(value.schemaVersion, value.version, value.archive, List.copyOf(value.tiers),
                Collections.unmodifiableMap(new LinkedHashMap<>(value.tierlists)));
    }

    private void validate() {
        if (schemaVersion != 1 || version == null || version.isBlank() || version.length() > 256
                || tierlists == null || tierlists.isEmpty() || tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("Invalid tier catalog");
        }
        if (archive == null || archive.sha256() == null || !archive.sha256().matches("[a-f0-9]{64}")
                || archive.path() == null || !archive.path().matches("tiertagger/[a-zA-Z0-9_/-]+\\.zip")
                || archive.path().contains("..") || archive.size() < 1 || archive.size() > AssetArchive.MAX_ARCHIVE) {
            throw new IllegalArgumentException("Invalid asset archive metadata");
        }
        int count = 0;
        for (var entry : tierlists.entrySet()) {
            String id = entry.getKey();
            Mode mode = entry.getValue();
            if (!id.matches("[a-z0-9][a-z0-9_]{0,63}") || mode == null || mode.displayName == null
                    || mode.displayName.isBlank() || mode.displayName.length() > 128
                    || mode.color == null || !mode.color.matches("#[0-9a-fA-F]{6}")
                    || mode.textIcon == null || mode.textIcon.length() > 32 || mode.badges == null) {
                throw new IllegalArgumentException("Invalid tier list: " + id);
            }
            validateGlyph(mode.icon, "assets/cistiertagger/textures/icons/" + id + ".png");
            count++;
            for (String tier : tiers) {
                if (!tier.matches("[a-z0-9][a-z0-9_]{0,31}")) throw new IllegalArgumentException("Invalid tier");
                validateGlyph(mode.badges.get(tier), "assets/cistiertagger/textures/badges/" + id + "/" + tier + ".png");
                count++;
            }
        }
        if (count > 6400) throw new IllegalArgumentException("Too many glyphs");
    }

    private static void validateGlyph(Glyph glyph, String path) {
        if (glyph == null || !path.equals(glyph.path) || !Character.isValidCodePoint(glyph.codepoint) || glyph.codepoint < 32
                || (glyph.codepoint >= 0xD800 && glyph.codepoint <= 0xDFFF)) {
            throw new IllegalArgumentException("Invalid glyph: " + path);
        }
    }

    public static TierCatalog empty() {
        return new TierCatalog(1, "", null, List.of(), Map.of());
    }
}
