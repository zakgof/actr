package com.zakgof.actr.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.Actr;

public class BasicTest {

	private final ActorSystem system = ActorSystem.create("nested");
	private final ActorRef<Simple> simple = system.actorOf(Simple::new);
	private final ActorRef<Master> master = system.actorOf(Master::new);
	private final ActorRef<FourtySeven> fourtySeven = system.actorOf(FourtySeven::new);
	private final ActorRef<Object> catcher = system.actorBuilder().constructor(Object::new).exceptionHandler((fs, e) -> caught(e)).build();

	private void caught(Exception exception) {
		assertEquals(catcher, Actr.current());
		assertEquals("oops", exception.getMessage());
		system.shutdown();
	}

	@Test
	public void testTell() {
		simple.tell(r -> r.run());
		system.shutdownCompletable().join();
	}

	private class Simple {
		public void run() {
			assertNull(Actr.caller());
			assertEquals(simple, Actr.current());
			system.shutdown();
		}
	}

	private class FourtySeven {
		public int run() {
			assertEquals(fourtySeven, Actr.current());
			return 47;
		}
	}

	@Test
	public void testAsk() {
		assertNull(Actr.caller());
		assertNull(Actr.current());
		master.tell(Master::run);
		system.shutdownCompletable().join();
	}

	@Test
	public void illegalAsk() {
		assertThrows(RuntimeException.class, () -> fourtySeven.ask(FourtySeven::run, i -> fail("Illegal ask returned a value")));
	}

	@Test
	public void throwingTell() {
		catcher.tell(fs -> {throw new RuntimeException("oops");});
		system.shutdownCompletable().join();
	}

	private class Master {

		public int run() {
			fourtySeven.ask(FourtySeven::run, this::recv);
			assertNull(Actr.caller());
			assertEquals(master, Actr.current());
			return 47;
		}

		private void recv(int result) {
			assertEquals(47, result);
			assertEquals(master, Actr.current());
			assertEquals(fourtySeven, Actr.caller());
			system.shutdown();
		}
	}

}
