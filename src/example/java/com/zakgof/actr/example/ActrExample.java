package com.zakgof.actr.example;

import java.util.Random;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;

public class ActrExample {
	
	
	
	public static void main(String[] args) throws InterruptedException {
		
		final ActorRef<Printer> printerActor = ActorRef.from(Printer::new);
		final ActorRef<Randomizer> randomizerActor = ActorRef.from(Randomizer::new);
		final ActorRef<Looper> looperActor = ActorRef.from(() -> new Looper(printerActor, randomizerActor));
		
		looperActor.tell(Looper::run);
		
		Thread.sleep(60000);
		ActorSystem.dflt().shutdown();
		
	}

	private static class Printer {
		public void print(String s) {
			System.err.println("[Printer] " + s);
		}
	}
	
	private static class Randomizer {
		
		private final Random random = new Random();
		
		public int random() {
			System.err.println("[Randomizer] >>> ");
			try {		
				return random.nextInt(10000);
			} finally {
				System.err.println("[Randomizer] <<< ");	
			}
		}
	}
	
	public static class Looper {

		private ActorRef<Printer> printerActor;
		private ActorRef<Randomizer> randomizerActor;

		public Looper(ActorRef<Printer> printerActor, ActorRef<Randomizer> randomizerActor) {
			this.printerActor = printerActor;
			this.randomizerActor = randomizerActor;
		}
		
		private void run() {
			printerActor.tell(printer -> printer.print("Looper: >>>"));
			randomizerActor.ask(Randomizer::random, this::showRandom);
			ActorSystem.dflt().callerActor(this).later(Looper::run, 1000);
			printerActor.tell(printer -> printer.print("Looper: <<<"));
		}
		
		private void showRandom(int rand) {
			printerActor.tell(printer -> printer.print("Looper: Randomizer returned: " + rand));
		}
		
	}

}
