package com.zakgof.actr.test;

import static com.zakgof.actr.test.BasicTest.CheckPoints.ActorCallReturning;
import static com.zakgof.actr.test.BasicTest.CheckPoints.ActorCallSimple;
import static com.zakgof.actr.test.BasicTest.CheckPoints.ActorCallThrowing;
import static com.zakgof.actr.test.BasicTest.CheckPoints.ActorConstructor;
import static com.zakgof.actr.test.BasicTest.CheckPoints.ActorDestructor;
import static com.zakgof.actr.test.BasicTest.CheckPoints.ExceptionHandler;
import static com.zakgof.actr.test.BasicTest.CheckPoints.FutureCompleted;
import static com.zakgof.actr.test.BasicTest.CheckPoints.FutureFailed;
import static com.zakgof.actr.test.BasicTest.CheckPoints.ResultReturned;
import static com.zakgof.actr.test.BasicTest.CheckPoints.validate;
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

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.Actr;
import com.zakgof.actr.Schedulers;

public class BasicTest {

	private final ActorSystem system = ActorSystem.create("test", Schedulers.newThreadPerActorScheduler());

	private final ActorRef<Master> master = system.<Master>actorBuilder()
		.constructor(Master::new)
		.build();
	private ActorRef<TestActor> testActor;

	@BeforeEach
	public void before() {
		CheckPoints.clean();

		testActor = system.<TestActor>actorBuilder()
			.constructor(TestActor::new)
			.destructor(TestActor::destructor)
			.exceptionHandler((fs, e) -> fs.err(e))
			.build();
	}

	@Test
	public void tell() {
		testActor.tell(TestActor::simple);
		system.shutdownCompletable().join();
		validate(ActorConstructor, ActorCallSimple, ActorDestructor);
	}

	@Test
	public void illegalAsk() {
		assertThrows(RuntimeException.class, () -> testActor.ask(TestActor::returning, i -> fail("Illegal ask returned a value")));
		system.shutdown().join();
		validate(ActorConstructor, ActorDestructor);
	}

	@Test
	public void ask() {
		assertNull(Actr.caller());
		assertNull(Actr.current());
		master.tell(Master::ask47);
		system.shutdownCompletable().join();
		validate(ActorConstructor, ActorCallReturning, ResultReturned, ActorDestructor);
	}

	@Test
	public void askFuture () {
		assertNull(Actr.caller());
		assertNull(Actr.current());
		master.tell(Master::askFuture);
		system.shutdownCompletable().join();
		validate(ActorConstructor, ActorCallReturning, ResultReturned, FutureCompleted, ActorDestructor);
	}

	@Test
	public void tellError() {
		master.tell(Master::tellError);
		system.shutdownCompletable().join();
		validate(ActorConstructor, ActorCallThrowing, ExceptionHandler, ActorDestructor);
	}

	@Test
	public void askError() {
		master.tell(Master::askError);
		system.shutdownCompletable().join();
		validate(ActorConstructor, ActorCallThrowing, ExceptionHandler, ActorDestructor);
	}

	@Test
	public void askErrorFuture() {
		master.tell(Master::askErrorFuture);
		system.shutdownCompletable().join();
		validate(ActorConstructor, ActorCallThrowing, FutureFailed, ActorDestructor);
	}

	private class Master {

		public void ask47() {
			testActor.ask(TestActor::returning, this::validateResult);
			assertNull(Actr.caller());
			assertEquals(master, Actr.current());
		}

		public void askFuture() {
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

		public void askErrorFuture() {
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

		public void askError() {
			testActor.ask(TestActor::throwing, value -> fail());
			assertNull(Actr.caller());
			assertEquals(master, Actr.current());
		}

		public void tellError() {
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
		FutureFailed;

		private static List<CheckPoints> checkpoints = new ArrayList<>();

		public static void clean() {
			checkpoints.clear();
		}

		public void check() {
			checkpoints.add(this);
		}

		public static void validate(CheckPoints... reference) {
			assertEquals(Arrays.asList(reference), checkpoints);
		}
	}

}
