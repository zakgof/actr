package com.zakgof.actr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DedicatedThreadScheduler implements IActorScheduler {

	private ExecutorService es;
	
	public DedicatedThreadScheduler() {
		this.es = Executors.newSingleThreadExecutor();
	}

	public DedicatedThreadScheduler(String name) {
		this.es = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "actr:" + name));
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
		try {
			es.awaitTermination(10000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
	}

}
