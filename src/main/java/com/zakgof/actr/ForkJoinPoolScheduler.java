package com.zakgof.actr;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class ForkJoinPoolScheduler implements IActorScheduler {

    private final ForkJoinPool pool;
    private final int throughput;
    private final Map<Object, Mailbox> delayed = new ConcurrentHashMap<>();
    private boolean shutdown = false;

    public ForkJoinPoolScheduler(int throughput) {
        this.pool = ForkJoinPool.commonPool();
        this.throughput = throughput;
    }

    private static class Mailbox {
        private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger queued = new AtomicInteger(0);
    }

    @Override
    public void schedule(Runnable raw, Object actorId) {

        if (shutdown) {
            return;
        }

        Runnable task = () -> {
            Mailbox mailbox = delayed.computeIfAbsent(actorId, k -> new Mailbox());
            mailbox.queue.add(raw);
            int before = mailbox.queued.getAndIncrement();
            // System.err.println("Add to mailbox, was: " + before);
            if (before == 0) {
                processMailbox(mailbox);
            }
        };
        pool.execute(task);
    }

    private void processMailbox(Mailbox mailbox) {
        // System.err.println("processMailbox with " + mailbox.queued.get() + "/" + mailbox.queue.size() + " " + Thread.currentThread().getName());
        int processed = 0;
        for (;;) {
            Runnable runnable = mailbox.queue.poll();
            if (runnable == null)
                break;
            runnable.run();
            processed++;
            if (processed >= throughput)
                break;
        }
        int remaining = mailbox.queued.addAndGet(-processed);
        // System.err.println(" Processed " + processed);
        if (remaining > 0) {
            // System.err.println(" Remaining in mailbox " + remaining);
            pool.execute(() -> processMailbox(mailbox));
        }
    }

    @Override
    public void destroy() {
        this.shutdown = true;
    }

}
