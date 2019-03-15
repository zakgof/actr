package com.zakgof.actr;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActorImpl<T> implements ActorRef<T> {
	
	private volatile T object;
	private final ActorSystem actorSystem;
	private final IActorScheduler scheduler;
	private final String name;
	private boolean removed;
	
	
	ActorImpl(T object, Supplier<T> constructor, Consumer<T> destructor, IActorScheduler scheduler, ActorSystem actorSystem, String name) {
		this.actorSystem = actorSystem;
		this.name = name;
		// System.err.println("    create actor " + name);
		if (object != null) {
			this.object = object;
		}
		this.scheduler = scheduler == null ? new DedicatedThreadScheduler() : scheduler;
		if (constructor != null) {
			Actr.setCurrent(this);
			this.object = constructor.get();
			Actr.setCurrent(null);
		}
		
		actorSystem.add(this);
	}

	@Override
	public void tell(Consumer<T> action) {
		ActorRef<?> caller = Actr.current();
		scheduler.schedule(() -> {
			Actr.setCurrent(this);
			Actr.setCaller(caller);
			action.accept(object);
			Actr.setCurrent(null);
			Actr.setCaller(null);
		}, this);
	}

	@Override
	public void later(Consumer<T> action, long ms) {
		ActorRef<?> caller = Actr.current();
		actorSystem.later(() -> 
			scheduler.schedule(() -> {
				Actr.setCurrent(this);
				Actr.setCaller(caller);
				action.accept(object);
				Actr.setCurrent(null);
				Actr.setCaller(null);
			}, this), ms
		);
	}
	
	
	@Override
	public <R> void ask(Function<T, R> call, Consumer<R> consumer) {
		tell(target -> {
			R result = call.apply(object);
			Actr.caller().tell(c -> consumer.accept(result));
		});
	}

	T object() {
		return object;
	}
	
	@Override
	public String toString() {
		return "[Actor: " + name + "]";
	}

	@Override
	public ActorSystem system() {
		return actorSystem;
	}

	public void destroy() {
		this.removed = true;
		actorSystem.remove(this);
	}

	@Override
	public <C> ActorRef<C> actorOf(Supplier<C> constructor, String name) {
		return system().actorOf(constructor, this.name + "/" + name);
	}

}