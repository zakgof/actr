package com.zakgof.actr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;

import com.zakgof.actr.impl.BlockingThreadScheduler;
import com.zakgof.actr.impl.ExecutorBasedScheduler;
import com.zakgof.actr.impl.SingleThreadScheduler;
import com.zakgof.actr.impl.ThreadPerActorScheduler;

public class Schedulers {

	public static IActorScheduler newForkJoinPoolScheduler(int throughput) {
		return new ExecutorBasedScheduler(ForkJoinPool.commonPool(), throughput);
	}

	public static IActorScheduler newExecutorBasedScheduler(ExecutorService executorService, int throughput) {
		return new ExecutorBasedScheduler(executorService, throughput);
	}

	public static IActorScheduler newFixedThreadPoolScheduler(int threads, int throughput) {
		return new ExecutorBasedScheduler(Executors.newFixedThreadPool(threads, runnable -> new Thread(runnable, "actr:fixed")), throughput);
	}

	public static BlockingThreadScheduler newBlockingThreadScheduler() {
		return new BlockingThreadScheduler();
	}

	public static IActorScheduler newSingleThreadScheduler() {
		return new SingleThreadScheduler();
	}

	public static IActorScheduler newThreadPerActorScheduler() {
		return new ThreadPerActorScheduler("actr");
	}

	public static IActorScheduler newThreadPerActorScheduler(ThreadFactory threadFactory) {
		return new ThreadPerActorScheduler(threadFactory);
	}

	public static IActorScheduler newThreadPerActorScheduler(String name) {
		return new ThreadPerActorScheduler(name);
	}

}
