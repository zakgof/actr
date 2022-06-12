package com.zakgof.actr.test;

import static com.zakgof.actr.test.BasicTest.CheckPoints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zakgof.actr.Actr;
import com.zakgof.actr.IActorRef;
import com.zakgof.actr.IActorSystem;
import com.zakgof.actr.Schedulers;

class BasicTest {

    private final IActorSystem system = Actr.newSystem("test", Schedulers.newThreadPerActorScheduler());

    private final IActorRef<Master> master = system.<Master>actorBuilder()
            .constructor(Master::new)
            .build();
    private IActorRef<TestActor> testActor;

    @BeforeEach
    void before() {
        CheckPoints.clean();

        testActor = system.<TestActor>actorBuilder()
                .constructor(TestActor::new)
                .destructor(TestActor::destructor)
                .exceptionHandler(TestActor::err)
                .build();
    }

    @Test
    void tell() {
        testActor.tell(TestActor::simple);
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallSimple, ActorDestructor);
    }

    @Test
    void ask() {
        assertNull(Actr.caller());
        assertNull(Actr.current());
        master.tell(Master::ask47);
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallReturning, ResultReturned, ActorDestructor);
    }

    @Test
    void askFuture() {
        assertNull(Actr.caller());
        assertNull(Actr.current());
        master.tell(Master::askFuture);
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallReturning, ResultReturned, FutureCompleted, ActorDestructor);
    }

    @Test
    void askFromNonActor() {
        testActor.ask(TestActor::returning, val -> {
            assertEquals(47, val);
            NonActorCallback.check();
            assertNull(Actr.caller());
            system.shutdown();
        });
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallReturning, NonActorCallback, ActorDestructor);
    }

    @Test
    void askFutureFromNonActor() {
        CompletableFuture<Integer> future = testActor.ask(TestActor::returning);
        future.thenAccept(val -> {
            assertEquals(47, val);
            NonActorCallback.check();
            assertNull(Actr.caller());
            system.shutdown();
        }).join();
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallReturning, NonActorCallback, ActorDestructor);
    }

    @Test
    void tellError() {
        master.tell(Master::tellError);
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallThrowing, ExceptionHandler, ActorDestructor);
    }

    @Test
    void askError() {
        master.tell(Master::askError);
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallThrowing, ExceptionHandler, ActorDestructor);
    }

    @Test
    void askErrorFuture() {
        master.tell(Master::askErrorFuture);
        system.shutdownCompletable().join();
        validate(ActorConstructor, ActorCallThrowing, FutureFailed, ActorDestructor);
    }

    private class Master {

        void ask47() {
            assertNull(Actr.caller());
            assertEquals(master, Actr.current());
            testActor.ask(TestActor::returning, this::validateResult);
            assertNull(Actr.caller());
            assertEquals(master, Actr.current());
        }

        void askFuture() {
            CompletableFuture<Integer> future = testActor.ask(TestActor::returning);
            assertNull(Actr.caller());
            assertEquals(master, Actr.current());

            future.thenAccept(value -> {
                validateResult(value);
                FutureCompleted.check();
            }).exceptionally(ex -> {
                FutureFailed.check();
                system.shutdown();
                return null;
            });
        }

        void askErrorFuture() {
            CompletableFuture<Integer> future = testActor.ask(TestActor::throwing);
            assertNull(Actr.caller());
            assertEquals(master, Actr.current());

            future.thenAccept(value -> fail()).exceptionally(ex -> {
                assertEquals(master, Actr.current());
                assertEquals(testActor, Actr.caller());
                FutureFailed.check();
                system.shutdown();
                return null;
            });
        }

        void askError() {
            testActor.ask(TestActor::throwing, value -> fail());
            assertNull(Actr.caller());
            assertEquals(master, Actr.current());
        }

        void tellError() {
            testActor.tell(TestActor::throwing);
            assertNull(Actr.caller());
            assertEquals(master, Actr.current());
        }

        private void validateResult(int result) {
            assertEquals(47, result);
            assertEquals(master, Actr.current());
            assertEquals(testActor, Actr.caller());
            ResultReturned.check();
            system.shutdown();
        }
    }

    private class TestActor {

        TestActor() {
            assertEquals(testActor, Actr.current());
            ActorConstructor.check();
        }

        void simple() {
            assertEquals(testActor, Actr.current());
            ActorCallSimple.check();
            system.shutdown();
        }

        int returning() {
            assertEquals(testActor, Actr.current());
            CheckPoints.ActorCallReturning.check();
            return 47;
        }

        int throwing() {
            CheckPoints.ActorCallThrowing.check();
            throw new RuntimeException("oops");
        }

        void err(Exception e) {
            assertEquals(testActor, Actr.current());
            ExceptionHandler.check();
            system.shutdown();
        }

        void destructor() {
            assertEquals(testActor, Actr.current());
            ActorDestructor.check();
        }

    }

    enum CheckPoints {
        ActorConstructor,
        ActorCallSimple,
        ActorCallReturning,
        ActorCallThrowing,
        ActorDestructor,
        ResultReturned,
        ExceptionHandler,
        FutureCompleted,
        FutureFailed,
        NonActorCallback;

        private static List<CheckPoints> checkpoints = new ArrayList<>();

        static void clean() {
            checkpoints.clear();
        }

        void check() {
            checkpoints.add(this);
        }

        static void validate(CheckPoints... reference) {
            assertEquals(Arrays.asList(reference), checkpoints);
        }
    }

}
