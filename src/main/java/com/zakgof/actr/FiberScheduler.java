package com.zakgof.actr;

import java.lang.FiberScope.Option;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class FiberScheduler implements IActorScheduler {

	private FiberScope scope = FiberScope.open(Option.CANCEL_AT_CLOSE);

    private Map<Object, FiberWorker> actorWorkers = new ConcurrentHashMap<>();

    @Override
    public void schedule(Runnable task, Object actorId) {
    	FiberWorker worker = actorWorkers.computeIfAbsent(actorId, FiberWorker::new);
    	worker.schedule(task);
    }

    @Override
    public void destroy() {

    }

    private class FiberWorker {

    	private Fiber<Void> fiber;
    	private BlockingQueue<Runnable> mailbox = new LinkedBlockingQueue<>();

    	private FiberWorker(Object actorId) {
    		fiber = scope.schedule(this::run);
    	}

    	public void schedule(Runnable task) {
			mailbox.add(task);
		}

		private void run() {
			for(;;) {
				try {
					Runnable task = mailbox.take();
					task.run();
				} catch (InterruptedException e) {
					return;
				}
			}
    	}

    }

}
