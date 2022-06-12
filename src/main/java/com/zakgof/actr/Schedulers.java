package com.zakgof.actr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;

import com.zakgof.actr.impl.BlockingThreadScheduler;
import com.zakgof.actr.impl.ExecutorBasedScheduler;
import com.zakgof.actr.impl.SingleThreadScheduler;
import com.zakgof.actr.impl.ThreadPerActorScheduler;

/**
 * Static factory to create schedulers.
 */
public class Schedulers {

    private Schedulers() {
    }

    /**
     * Creates a scheduler based on the shared ForkJoinPool.
     * @param throughput maximum number of pending actor messages to be processed at once
     * @return scheduler
     */
    public static IActorScheduler newForkJoinPoolScheduler(int throughput) {
        return new ExecutorBasedScheduler(ForkJoinPool.commonPool(), throughput);
    }

    /**
     * Creates a scheduler based on a user-provided ExecuterService.
     * @param executorService executor service for scheduling the tasks
     * @param throughput maximum number of pending actor messages to be processed at once
     * @return scheduler
     */
    public static IActorScheduler newExecutorBasedScheduler(ExecutorService executorService, int throughput) {
        return new ExecutorBasedScheduler(executorService, throughput);
    }

    /**
     * Creates a scheduler based on a thread pool with a fixed number of threads.
     * @param threads number of threads in the thread pool
     * @param throughput maximum number of pending actor messages to be processed at once
     * @return scheduler
     */
    public static IActorScheduler newFixedThreadPoolScheduler(int threads, int throughput) {
        return new ExecutorBasedScheduler(Executors.newFixedThreadPool(threads, runnable -> new Thread(runnable, "actr:fixed")), throughput);
    }

    /**
     * Creates a scheduler that processed all the actors messages sequentially in a single
     * user-supplied thread. See {@link BlockingThreadScheduler} for details.
     * @return scheduler
     */
    public static BlockingThreadScheduler newBlockingThreadScheduler() {
        return new BlockingThreadScheduler();
    }

    /**
     * Creates a scheduler using a single thread for all actor calls.
     * @return scheduler
     */
    public static IActorScheduler newSingleThreadScheduler() {
        return new SingleThreadScheduler();
    }

    /**
     * Creates a scheduler that creates a single-thread executor for each actor.
     * @return scheduler
     */
    public static IActorScheduler newThreadPerActorScheduler() {
        return new ThreadPerActorScheduler("actr");
    }

    /**
     * Creates a scheduler that creates a single-thread executor for each actor.
     * @param threadFactory thread factory to be used for thread creation
     * @return scheduler
     */
    public static IActorScheduler newThreadPerActorScheduler(ThreadFactory threadFactory) {
        return new ThreadPerActorScheduler(threadFactory);
    }

    /**
     * Creates a scheduler that creates a single-thread executor for each actor.
     * @param name thread name
     * @return scheduler
     */
    public static IActorScheduler newThreadPerActorScheduler(String name) {
        return new ThreadPerActorScheduler(name);
    }

}
