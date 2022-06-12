package com.zakgof.actr.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.zakgof.actr.Actr;
import com.zakgof.actr.IActorRef;
import com.zakgof.actr.IActorSystem;

class NestedAskTest {

    private final IActorSystem system = Actr.newSystem("nested");

    private final IActorRef<Master> master = system.actorOf(Master::new);
    private final IActorRef<Runner1> runner1 = system.actorOf(Runner1::new);
    private final IActorRef<Runner2> runner2 = system.actorOf(Runner2::new);

    private final IActorRef<FourtySeven> fourtySeven = system.actorOf(FourtySeven::new);

    @Test
    void testNestedAsk() {
        master.tell(Master::run);
        system.shutdownCompletable().join();
    }

    private class Master {

        void run() {
            runner1.ask(Runner1::run, this::recv);
        }

        private void recv(int result) {
            assertEquals(47, result);
            assertEquals(master, Actr.current());
            assertEquals(runner1, Actr.caller());
            system.shutdown();
        }
    }

    private class Runner1 {
        void run(Consumer<Integer> callback) {
            assertEquals(runner1, Actr.current());
            assertEquals(master, Actr.caller());
            runner2.ask(Runner2::run, i -> {
                assertEquals(runner1, Actr.current());
                assertEquals(runner2, Actr.caller());
                callback.accept(i);
            });
        }
    }

    private class Runner2 {
        void run(Consumer<Integer> callback) {
            assertEquals(runner2, Actr.current());
            assertEquals(runner1, Actr.caller());
            fourtySeven.ask((r, c) -> c.accept(r.run()), (Integer i) -> {
                assertEquals(runner2, Actr.current());
                assertEquals(fourtySeven, Actr.caller());
                callback.accept(i);
            });
        }
    }

    private class FourtySeven {
        int run() {
            assertEquals(fourtySeven, Actr.current());
            assertEquals(runner2, Actr.caller());
            return 47;
        }
    }

}
