package com.zakgof.actr.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.zakgof.actr.IActorScheduler;

/**
 * Scheduler that creates a single-thread executor for each actor.
 */
public class ThreadPerActorScheduler implements IActorScheduler {

    private Map<Object, ExecutorService> executors = new ConcurrentHashMap<>();
    private ThreadFactory threadFactory;

    public ThreadPerActorScheduler() {
        this(Thread::new);
    }

    public ThreadPerActorScheduler(String name) {
        this(runnable -> new Thread(runnable, "actr:" + name));
    }

    public ThreadPerActorScheduler(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    @Override
    public void actorCreated(Object actorId) {
        executors.put(actorId, Executors.newSingleThreadExecutor(threadFactory));
    }

    @Override
    public void actorDisposed(Object actorId) {
        ExecutorService service = executors.remove(actorId);
        service.shutdown();
    }

    @Override
    public void schedule(Runnable task, Object actorId) {
        ExecutorService executor = executors.get(actorId);
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    @Override
    public void close() {
        executors.values().forEach(ExecutorService::shutdown);
    }
}
