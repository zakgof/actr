package com.zakgof.actr;

import java.util.concurrent.ThreadFactory;

public class Schedulers {

	public static IActorScheduler newForkJoinPoolScheduler(int throughput) {
		return new ForkJoinPoolScheduler(throughput);
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
