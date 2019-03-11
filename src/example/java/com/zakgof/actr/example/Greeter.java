package com.zakgof.actr.example;

import com.zakgof.actr.ActorRef;

public class Greeter {

	private String message;
	private ActorRef<Printer> printerActor;
	private String greeting;

	public Greeter(String message, ActorRef<Printer> printerActor) {
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
