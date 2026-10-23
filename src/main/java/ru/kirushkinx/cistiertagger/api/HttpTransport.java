package ru.kirushkinx.cistiertagger.api;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import ru.kirushkinx.cistiertagger.CisTierTagger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@UtilityClass
public class HttpTransport {

    @Getter
    private static final String userAgent = "CISTierTagger/" + FabricLoader.getInstance()
            .getModContainer(CisTierTagger.MOD_ID)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown").split("\\+", 2)[0];

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static HttpResponse<byte[]> get(URI uri, int timeoutMs, int limit, String etag) throws Exception {
        var builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .header("User-Agent", userAgent)
                .header("Accept-Encoding", "gzip")
                .GET();
        if (etag != null && !etag.isBlank()) builder.header("If-None-Match", etag);
        var pending = HTTP.sendAsync(builder.build(), info -> new LimitedBody(limit));
        try {
            var response = pending.get(timeoutMs, TimeUnit.MILLISECONDS); // whole response timeout
            if (response.statusCode() != 304 && response.statusCode() / 100 != 2) {
                throw new HttpStatusException(response.statusCode(), uri);
            }
            return response;
        } catch (Exception e) {
            pending.cancel(true);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw e;
        }
    }

    public static byte[] body(HttpResponse<byte[]> response, int limit) throws IOException {
        if (!response.headers().firstValue("Content-Encoding").orElse("").equalsIgnoreCase("gzip")) return response.body();
        try (var in = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            byte[] bytes = in.readNBytes(limit + 1);
            if (bytes.length > limit) throw new IOException("Decompressed response too large");
            return bytes;
        }
    }

    @Getter
    public static final class HttpStatusException extends IOException {

        private final int status;

        public HttpStatusException(int status, URI uri) {
            super("HTTP " + status + " from " + uri);
            this.status = status;
        }
    }

    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {

        private final int limit;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;

        private LimitedBody(int limit) {
            this.limit = limit;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            value.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > limit - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException("Response too large"));
                    return;
                }
                byte[] part = new byte[buffer.remaining()];
                buffer.get(part);
                bytes.writeBytes(part);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable error) {
            result.completeExceptionally(error);
        }

        @Override
        public void onComplete() {
            result.complete(bytes.toByteArray());
        }
    }
}
