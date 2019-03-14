package com.zakgof.actr;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActorImpl<T> implements ActorRef<T> {
	
	private T object;
	private final ActorSystem actorSystem;
	private final ActorScheduler scheduler;
	private String name;
	
	
	
	ActorImpl(T object, Supplier<T> constructor, Consumer<T> destructor, ActorScheduler scheduler, ActorSystem actorSystem, String name) {
		this.actorSystem = actorSystem;
		this.name = name;
		if (object != null) {
			this.object = object;
			actorSystem.add(this);
		}
		this.scheduler = scheduler == null ? new DedicatedThreadScheduler() : scheduler;
		if (constructor != null) {
			this.scheduler.schedule(() -> {
				actorSystem.setCurrentActor(this);
				this.object = constructor.get();
				actorSystem.add(this);
			}, this);
		}
		
	}

	@Override
	public void tell(Consumer<T> action) {
		scheduler.schedule(() -> {
			actorSystem.setCurrentActor(this);
			action.accept(object);
			actorSystem.setCurrentActor(null);
		}, this);
	}
	
	
	@Override
	public <R> void ask(Function<T, R> call, Consumer<R> consumer) {
		ActorRef<?> caller = actorSystem.callerActor();
		scheduler.schedule(() -> {
			actorSystem.setCurrentActor(this);
			R result = call.apply(object);
				caller.tell(c -> consumer.accept(result));
			actorSystem.setCurrentActor(null);
		}, this);
	}

	T object() {
		return object;
	}
	
	@Override
	public String toString() {
		return "[Actor: " + object.getClass().getSimpleName() + "/" + name + "]";
	}

	@Override
	public ActorSystem system() {
		return actorSystem;
	}

	public void destroy() {
		// TODO Auto-generated method stub
		
	}

}