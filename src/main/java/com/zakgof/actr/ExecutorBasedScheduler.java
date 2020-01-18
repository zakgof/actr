package com.zakgof.actr;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler based on a provided executor service.
 */
public class ExecutorBasedScheduler implements IActorScheduler {

	private final int throughput;
	private final Map<Object, Mailbox> mailboxes = new ConcurrentHashMap<>();
	private final ExecutorService executor;

	private volatile boolean shutdown = false;

	public ExecutorBasedScheduler(ExecutorService executor, int throughput) {
		this.executor = executor;
		this.throughput = throughput;
	}

	private static class Mailbox {
		private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
		private final AtomicInteger queued = new AtomicInteger(0);
	}

	@Override
	public void actorCreated(Object actorId) {
		mailboxes.put(actorId, new Mailbox());
	}

	@Override
	public void actorDisposed(Object actorId) {
		mailboxes.remove(actorId);
	}

	@Override
	public void schedule(Runnable raw, Object actorId) {

		if (shutdown) {
			return;
		}

		Runnable task = () -> {
			Mailbox mailbox = mailboxes.get(actorId);
			mailbox.queue.add(raw);
			int before = mailbox.queued.getAndIncrement();
			// System.err.println("Add to mailbox, was: " + before);
			if (before == 0) {
				processMailbox(mailbox);
			}
		};
		executor.execute(task);
	}

	private void processMailbox(Mailbox mailbox) {
		// System.err.println("processMailbox with " + mailbox.queued.get() + "/" +
		// mailbox.queue.size() + " " + Thread.currentThread().getName());
		int processed = 0;
		for (;;) {
			Runnable runnable = mailbox.queue.poll();
			if (runnable == null)
				break;
			runnable.run();
			processed++;
			if (processed >= throughput)
				break;
		}
		int remaining = mailbox.queued.addAndGet(-processed);
		// System.err.println(" Processed " + processed);
		if (remaining > 0) {
			// System.err.println(" Remaining in mailbox " + remaining);
			executor.execute(() -> processMailbox(mailbox));
		}
	}

	@Override
	public void close() {
		this.shutdown = true;
		executor.shutdown();
	}

}
