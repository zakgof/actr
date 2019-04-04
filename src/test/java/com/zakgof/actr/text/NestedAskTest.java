package com.zakgof.actr.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.Actr;

public class NestedAskTest {
	
	private final ActorSystem system = ActorSystem.create("nested");
	
	private final ActorRef<Master> master = system.actorOf(Master::new);
	private final ActorRef<Runner1> runner1 = system.actorOf(Runner1::new);
	private final ActorRef<Runner2> runner2 = system.actorOf(Runner2::new);
	
	
	private final ActorRef<FourtySeven> fourtySeven = system.actorOf(FourtySeven::new);
	
	
	@Test
	public void testNestedAsk() {
		master.tell(r -> r.run());
		system.shutdownCompletable().join();
	}
	
	private class Master {

		public void run() {
			runner1.ask((r, c) -> r.run(c), this::recv);
		}
		
		private void recv(int result) {
			assertEquals(47, result);
			assertEquals(master, Actr.current());
			assertEquals(runner1, Actr.caller());
			system.shutdown();
		}
	}
	
	private class Runner1 {
		public void run(Consumer<Integer> callback) {
			assertEquals(runner1, Actr.current());
			assertEquals(master, Actr.caller());
			runner2.ask((r, c) -> r.run(c), (Integer i) -> {
				assertEquals(runner1, Actr.current());
				assertEquals(runner2, Actr.caller());
				callback.accept(i);	
			});
		}
	}
	
	private class Runner2 {
		public void run(Consumer<Integer> callback) {
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
		public int run() {
			assertEquals(fourtySeven, Actr.current());
			assertEquals(runner2, Actr.caller());
			return 47;
		}
	}
	
	
}
