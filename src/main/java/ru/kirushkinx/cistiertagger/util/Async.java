package ru.kirushkinx.cistiertagger.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class Async {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    public static @NotNull ThreadFactory daemonThreadFactory(@NotNull String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + COUNTER.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) ->
                LoggerFactory.getLogger(Async.class).error("Uncaught error in " + t.getName(), e));
            return thread;
        };
    }

    public static @NotNull ExecutorService daemonExecutor(int poolSize, @NotNull String prefix) {
        return Executors.newFixedThreadPool(poolSize, daemonThreadFactory(prefix));
    }

    public static @NotNull ScheduledExecutorService daemonScheduler(int poolSize, @NotNull String prefix) {
        return Executors.newScheduledThreadPool(poolSize, daemonThreadFactory(prefix));
    }
}
