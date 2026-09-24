package io.xacml.pep.json.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs the same call from many threads at once, to check that a shared client instance is thread-safe.
 */
public final class ConcurrentCalls {

    private ConcurrentCalls() {
    }

    /**
     * @return a description of every failed call; empty when all calls returned {@code true}
     */
    public static List<String> run(int threads, int calls, Callable<Boolean> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < calls; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        if (!call.call()) {
                            failures.add("unexpected result");
                        }
                    } catch (Exception e) {
                        failures.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        return new ArrayList<>(failures);
    }
}
