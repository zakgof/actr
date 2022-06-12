package com.zakgof.actr.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.zakgof.actr.Actr;
import com.zakgof.actr.IActorRef;
import com.zakgof.actr.IActorScheduler;
import com.zakgof.actr.IActorSystem;
import com.zakgof.actr.Schedulers;

class ShutdownTest {

    private final IActorSystem system = Actr.newSystem("massive");
    private final IActorRef<Runnable> shutdown = system.actorOf(() -> system::shutdown);

    @Test
    void massiveShutdown() throws InterruptedException {
        for (int i = 0; i < 100000; i++) {
            IActorRef<Runnable> noop = system.actorOf(() -> Thread::yield);
            noop.tell(Runnable::run);
        }
        Thread.sleep(100);
        shutdown.tell(Runnable::run);
        system.shutdownCompletable().join();
    }

    // @Test
    void shutdownDedicated() throws InterruptedException {
        int initialThreads = Thread.activeCount();
        IActorScheduler scheduler = Schedulers.newThreadPerActorScheduler();
        IActorRef<Yielder> d1 = system.<Yielder> actorBuilder().constructor(Yielder::new).scheduler(scheduler).build();
        IActorRef<Yielder> d2 = system.<Yielder> actorBuilder().constructor(Yielder::new).scheduler(scheduler).build();
        IActorRef<Yielder> d3 = system.<Yielder> actorBuilder().constructor(Yielder::new).scheduler(scheduler).build();
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
