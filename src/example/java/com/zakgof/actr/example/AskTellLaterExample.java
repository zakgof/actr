package com.zakgof.actr.example;

import java.util.Random;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.Actr;

public class AskTellLaterExample {
	
	
	
	public static void main(String[] args) throws InterruptedException {
		ActorSystem system = ActorSystem.create("example");
		final ActorRef<Printer> printerActor = system.actorOf(Printer::new);
		final ActorRef<Randomizer> randomizerActor = system.actorOf(Randomizer::new);
		final ActorRef<Looper> looperActor = system.actorOf(() -> new Looper(printerActor, randomizerActor));
		
		looperActor.tell(Looper::run);
		
		system.shutdownCompletable().join();
		
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

		private final ActorRef<Printer> printerActor;
		private final ActorRef<Randomizer> randomizerActor;
		
		private int iteration = 0;

		public Looper(ActorRef<Printer> printerActor, ActorRef<Randomizer> randomizerActor) {
			this.printerActor = printerActor;
			this.randomizerActor = randomizerActor;
		}
		
		private void run() {
			iteration++;
			printerActor.tell(printer -> printer.print("Looper: iteration " + iteration));
			randomizerActor.ask(Randomizer::random, this::showRandom);
			if (iteration < 10) {
				Actr.<Looper>current().later(Looper::run, 1000);
			} else {
				Actr.system().shutdown();
			}
		}
		
		private void showRandom(int rand) {
			printerActor.tell(printer -> printer.print("Looper: Randomizer returned: " + rand));
		}
		
	}

}
