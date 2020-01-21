package com.zakgof.actr.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.zakgof.actr.IActorScheduler;

/**
 * Scheduler that processed all the actors messages sequentially in a
 * single-thread executor service.
 */
public class SingleThreadScheduler implements IActorScheduler {

	private final ExecutorService executor;

	public SingleThreadScheduler() {
		this.executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "actr:single"));
	}

	@Override
	public void actorCreated(Object actorId) {
	}

	@Override
	public void actorDisposed(Object actorId) {
	}

	@Override
	public void schedule(Runnable task, Object actorId) {
		if (!executor.isShutdown() && !executor.isTerminated()) {
			executor.submit(task);
		}
	}

	@Override
	public void close() {
		executor.shutdown();
	}

}
