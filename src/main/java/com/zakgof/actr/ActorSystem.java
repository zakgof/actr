package com.zakgof.actr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ActorSystem {

	private static final ActorSystem DEFAULT = new ActorSystem("default");
	private String name;
	private Map<Object, ActorRef<?>> actors = new ConcurrentHashMap<>();
	private ThreadLocal<ActorRef<?>> currentActor = new ThreadLocal<>();

	public ActorSystem(String name) {
		this.name = name;
	}

	public static ActorSystem dflt() {
		return DEFAULT;
	}

	public static ActorSystem create(String name) {
		return new ActorSystem(name);
	}

	public <T> ActorRef<T> actorOf(Supplier<T> constructor, String name) {
		return ActorRef.<T>builder(this).constructor(constructor).threadName(name).build();
	}

	public void shutdown() { // TODO : thread safety !!!
		for (ActorRef<?> actorRef : actors.values()) {
			actorRef.destroy();
		}
	}

	void add(ActorRef<?> actorRef) {
		actors.put(actorRef.object(), actorRef);
	}

//	@SuppressWarnings("unchecked")
//	public <T> ActorRef<T> actorOf(T object) {
//		return (ActorRef<T>) actors.get(object);
//	}

	public <T> ActorRef<T> callerActor(T object) {
		return (ActorRef<T>) currentActor.get();
	}
	
	ActorRef<?> callerActor() {
		return currentActor.get();
	}

	void setCurrentActor(ActorRef<?> actorRef) {
		currentActor.set(actorRef);
	}

}
