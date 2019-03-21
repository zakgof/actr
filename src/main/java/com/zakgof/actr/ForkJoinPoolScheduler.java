package com.zakgof.actr;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForkJoinPoolScheduler implements IActorScheduler {
	
	
	private ForkJoinPool pool;
	private Map<Object, Mailbox> delayed = new ConcurrentHashMap<>();

	public ForkJoinPoolScheduler() {
		pool = ForkJoinPool.commonPool();
	}
	
	private static class Mailbox {
		private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
		private final AtomicBoolean locked = new AtomicBoolean();
	}

	@Override
	public void schedule(Runnable raw, Object actorId) {
		
		Runnable task = () -> {
			Mailbox mailbox = delayed.compute(actorId, (a, l) -> {if (l == null) return new Mailbox(); else return l;});
			// System.err.println("Enqueue " + raw + "  queue" + queue.size() + " " + Thread.currentThread().getName());
			mailbox.queue.add(raw);
			if (!mailbox.locked.compareAndSet(false, true)) {
				// System.err.println( "leave it " + Thread.currentThread().getName());
				return;
			}
			for(;;) {
				Runnable runnable = mailbox.queue.peek();
				if (runnable != null) {
					// System.err.println("         run " + runnable + "  queue" + queue.size() + " " + Thread.currentThread().getName());
					runnable.run();
					mailbox.queue.remove();
				} else
					break;
			}
			mailbox.locked.set(false);
			// System.err.println( "finish " + Thread.currentThread().getName());
		};
		pool.execute(task);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}


}
