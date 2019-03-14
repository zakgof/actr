package com.zakgof.actr;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ActorSystem {

	private static final ActorSystem DEFAULT = new ActorSystem("default");
	private String name;
	private Map<Object, ActorImpl<?>> actors = new ConcurrentHashMap<>();
	private static ThreadLocal<ActorImpl<?>> currentActor = new ThreadLocal<>();

	public ActorSystem(String name) {
		this.name = name;
	}

	public static ActorSystem dflt() {
		return DEFAULT;
	}

	public static ActorSystem create(String name) {
		return new ActorSystem(name);
	}

	public void shutdown() { // TODO : thread safety !!!
		for (ActorImpl<?> actorRef : actors.values()) {
			actorRef.destroy();
		}
	}

	void add(ActorImpl<?> actorRef) {
		actors.put(actorRef.object(), actorRef);
	}

//	@SuppressWarnings("unchecked")
//	public <T> ActorRef<T> actorOf(T object) {
//		return (ActorRef<T>) actors.get(object);
//	}

	public static <T> ActorRef<T> callerActor(T object) {
		return (ActorRef<T>) currentActor.get();
	}
	
	ActorRef<?> callerActor() {
		return currentActor.get();
	}

	void setCurrentActor(ActorImpl<?> actorRef) {
		currentActor.set(actorRef);
	}

	public <T> ActorBuilder<T> actorBuilder() {
		return new ActorBuilder<T>(this);
	}	

	public <T> ActorRef<T> actorOf(Supplier<T> constructor, String name) {
		return this.<T>actorBuilder().constructor(constructor).name(name).build();
	}

	public <T> ActorRef<T> actorOf(Supplier<T> constructor) {
		return actorOf(constructor, Long.toHexString(new Random().nextLong()));
	}


	public static class ActorBuilder<T> {
		private ActorSystem actorSystem;
		private T object;
		private Supplier<T> constructor;
		private Consumer<T> destructor;
		private ActorScheduler scheduler;
		private String name;

		public ActorBuilder(ActorSystem actorSystem) {
			this.actorSystem = actorSystem;
		}
		
		public ActorBuilder<T> object(T object) {
			this.object = object;
			return this;
		}
		
		public ActorBuilder<T> constructor(Supplier<T> constructor) {
			this.constructor = constructor;
			return this;
		}

		public ActorBuilder<T> name(String name) {
			this.name = name;
			return this;
		}
		
		public ActorBuilder<T> destructor(Consumer<T> destructor) {
			this.destructor = destructor;
			return this;
		}
		
		public ActorBuilder<T> scheduler(ActorScheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}
		
		public ActorRef<T> build() {
			if (constructor != null && object != null)
				throw new IllegalArgumentException("Not allowed to provide both object and constructor");
			if (constructor == null && object == null)
				throw new IllegalArgumentException("Provide either object or constructor");
			
			ActorRef<T> actorRef = new ActorImpl<T>(object, constructor, destructor, scheduler, actorSystem, name);
			return actorRef;
		}

	}


}
