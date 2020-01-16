package com.zakgof.actr.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.IActorScheduler;
import com.zakgof.actr.Schedulers;

public class ShutdownTest {

	private final ActorSystem system = ActorSystem.create("massive");
	private final ActorRef<Runnable> shutdown = system.actorOf(() -> system::shutdown);

	@Test
	public void massiveShutdown() throws InterruptedException {
		for (int i=0; i<100000; i++) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Thread.yield();
				}
			};
			ActorRef<Runnable> noop = system.actorOf(() -> runnable);
			noop.tell(Runnable::run);
		}
		Thread.sleep(100);
		shutdown.tell(Runnable::run);
		system.shutdownCompletable().join();
	}


	@Test
	public void shutdownDedicated() throws InterruptedException {
		int initialThreads = Thread.activeCount();
		IActorScheduler scheduler = Schedulers.newThreadPerActorScheduler();
		ActorRef<Yielder> d1 = system.<Yielder>actorBuilder().constructor(Yielder::new).scheduler(scheduler).build();
		ActorRef<Yielder> d2 = system.<Yielder>actorBuilder().constructor(Yielder::new).scheduler(scheduler).build();
		ActorRef<Yielder> d3 = system.<Yielder>actorBuilder().constructor(Yielder::new).scheduler(scheduler).build();
		d1.tell(Runnable::run);
		d2.tell(Runnable::run);
		d3.tell(Runnable::run);

		Thread.sleep(100);
		assertEquals(initialThreads + 3, Thread.activeCount());
		system.shutdown().join();
		Thread.sleep(100);
		scheduler.close();
		Thread.sleep(100);
		assertEquals(initialThreads, Thread.activeCount());

	}

	private static class Yielder implements Runnable {
		@Override
		public void run() {
			Thread.yield();
		}
	}

}
