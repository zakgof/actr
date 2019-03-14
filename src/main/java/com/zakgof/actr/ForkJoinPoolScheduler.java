package com.zakgof.actr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

public class ForkJoinPoolScheduler implements ActorScheduler {
	
	
	private ForkJoinPool pool;
	private Map<Object, ConcurrentLinkedQueue<Runnable>> delayed = new ConcurrentHashMap<>();

	public ForkJoinPoolScheduler() {
		pool = ForkJoinPool.commonPool();
	}

	@Override
	public void schedule(Runnable raw, Object actorId) {
		
		Runnable task = () -> {
			// System.err.println(">>>> runnin " + actorId + " in " + Thread.currentThread().getName() + " " + raw);
			ConcurrentLinkedQueue<Runnable> queue = delayed.compute(actorId, (a, l) -> {if (l == null) return new ConcurrentLinkedQueue<>(); else return l;});
			queue.add(raw);
			if (queue.peek() != raw) {
				// System.err.println("    delayed " + raw + "  :  " + queue);
				return;
			}
			for(;;) {
				Runnable runnable = queue.peek();
				if (runnable != null) {
					// System.err.println("    run " + runnable);
					runnable.run();
					queue.poll();
				} else
					break;
			}
			
			// System.err.println("<<<< runnin " + actorId + " in " + Thread.currentThread().getName());
		};
		pool.execute(task);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

}
