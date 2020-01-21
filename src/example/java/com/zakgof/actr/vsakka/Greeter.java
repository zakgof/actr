package com.zakgof.actr.vsakka;

import com.zakgof.actr.IActorRef;

public class Greeter {

	private String message;
	private IActorRef<Printer> printerActor;
	private String greeting;

	public Greeter(String message, IActorRef<Printer> printerActor) {
		this.message = message;
		this.printerActor = printerActor;
	}
	
	public void setWhoToGreet(String whoToGreet) {
		this.greeting = message + ", " + whoToGreet;
	}
	
	public void greet() {
		String greetingMsg = greeting;
		printerActor.tell(printer -> printer.print(greetingMsg));
	}

}
