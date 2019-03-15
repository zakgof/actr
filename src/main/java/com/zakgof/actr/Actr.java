package com.zakgof.actr;

public class Actr {
	
	private static ThreadLocal<ActorRef<?>> currentActor = new ThreadLocal<>();
	private static ThreadLocal<ActorRef<?>> callerActor = new ThreadLocal<>();
	
	@SuppressWarnings("unchecked")
	public static <T> ActorRef<T> current() {
		return (ActorRef<T>)currentActor.get();
	}
	
	// TODO: identify behavior for ask reply !
	@SuppressWarnings("unchecked")
	public static <T> ActorRef<T> caller() {
		return (ActorRef<T>)callerActor.get();
	}
	
	public static ActorSystem system() {
		return current().system();
	}
	
	static void setCurrent(ActorRef<?> actor) {
		currentActor.set(actor);
	}
	
	static void setCaller(ActorRef<?> actor) {
		callerActor.set(actor);
	}

}
