package com.zakgof.actr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadScheduler implements IActorScheduler {

    private ExecutorService es;

    public VirtualThreadScheduler() {
        this.es = Executors.newSingleThreadExecutor();
    }

    public VirtualThreadScheduler(String name) {
        this.es = Executors.newSingleThreadExecutor(runnable -> Thread.newThread("actr:" + name, Thread.VIRTUAL, runnable));
    }

    @Override
    public void schedule(Runnable task, Object actorId) {
        if (!es.isShutdown()) {
            es.execute(task);
        }
    }

    @Override
    public void destroy() {
        es.shutdown();
    }

}
