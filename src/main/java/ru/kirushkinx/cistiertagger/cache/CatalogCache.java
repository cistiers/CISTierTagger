package ru.kirushkinx.cistiertagger.cache;

import ru.kirushkinx.cistiertagger.api.HttpTransport;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class CatalogCache {

    private final Map<URI, Entry> entries = new HashMap<>();

    private record Entry(String etag, long expires, byte[] body) {}

    public byte[] get(URI uri, int timeout, Consumer<byte[]> validate) throws Exception {
        return get(uri, timeout, validate, false);
    }

    public byte[] get(URI uri, int timeout, Consumer<byte[]> validate, boolean revalidate) throws Exception {
        Entry cached = entries.get(uri);
        if (cached != null && !revalidate && cached.expires > System.currentTimeMillis()) return cached.body;
        var response = HttpTransport.get(uri, timeout, 4 * 1024 * 1024, cached == null ? null : cached.etag);
        byte[] body = response.statusCode() == 304 && cached != null ? cached.body : HttpTransport.body(response, 4 * 1024 * 1024);
        validate.accept(body);
        String cacheControl = response.headers().firstValue("Cache-Control").orElse("");
        var match = Pattern.compile("(?:^|[, ]+)max-age=(\\d+)").matcher(cacheControl);
        long maxAge = match.find() ? Math.min(86400, Long.parseLong(match.group(1))) : 300;
        long age = Long.parseLong(response.headers().firstValue("Age").orElse("0"));
        if (cacheControl.contains("no-cache")) maxAge = 0;
        if (!cacheControl.contains("no-store")) {
            String etag = response.headers().firstValue("ETag").orElse(cached == null ? null : cached.etag);
            entries.put(uri, new Entry(etag, System.currentTimeMillis() + Math.max(0, maxAge - age) * 1000, body));
        } else entries.remove(uri);
        return body;
    }

    public void retain(Collection<URI> urls) {
        entries.keySet().retainAll(urls);
    }

    public static void write(Path path, byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "download-", ".tmp");
        try {
            Files.write(temporary, bytes);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
