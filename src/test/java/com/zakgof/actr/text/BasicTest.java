package com.zakgof.actr.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.Actr;

public class BasicTest {
	
	private final ActorSystem system = ActorSystem.create("nested");
	private final ActorRef<Simple> simple = system.actorOf(Simple::new);
	private final ActorRef<Master> master = system.actorOf(Master::new);
	private final ActorRef<FourtySeven> fourtySeven = system.actorOf(FourtySeven::new);
	
	
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
