package com.zakgof.actr;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingThreadScheduler implements IActorScheduler {

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private Thread thread;

    @Override
    public void schedule(Runnable task, Object actorId) {
        queue.add(task);
    }

    @Override
    public void close() {
        thread.interrupt();
    }

    public void start() {
        this.thread = Thread.currentThread();
        try {
            while (!thread.isInterrupted()) {
                Runnable job = queue.take();
                job.run();
            }
        } catch (InterruptedException e) {
        }
    }

}
