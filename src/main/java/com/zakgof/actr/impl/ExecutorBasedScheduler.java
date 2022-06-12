package com.zakgof.actr.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.zakgof.actr.IActorScheduler;

/**
 * Scheduler based on a provided executor service.
 */
public class ExecutorBasedScheduler implements IActorScheduler {

    private final int throughput;
    private final ExecutorService executor;

    private volatile boolean shutdown = false;

    public ExecutorBasedScheduler(ExecutorService executor, int throughput) {
        this.executor = executor;
        this.throughput = throughput;
    }

    private static class Mailbox {
        private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger queued = new AtomicInteger(0);
    }

    @Override
    public void actorCreated(Object actorId) {
        ((ActorImpl<?>) actorId).box(new Mailbox());
    }

    @Override
    public void actorDisposed(Object actorId) {
        ((ActorImpl<?>) actorId).box(null);
    }

    @Override
    public void schedule(Runnable raw, Object actorId) {

        if (shutdown) {
            return;
        }

        Runnable task = () -> {
            Mailbox mailbox = (Mailbox) ((ActorImpl<?>) actorId).box();
            mailbox.queue.add(raw);
            int before = mailbox.queued.getAndIncrement();
            if (before == 0) {
                processMailbox(mailbox);
            }
        };
        executor.execute(task);
    }

    private void processMailbox(Mailbox mailbox) {
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
        if (remaining > 0) {
            executor.execute(() -> processMailbox(mailbox));
        }
    }

    @Override
    public void close() {
        this.shutdown = true;
        executor.shutdown();
    }

}
