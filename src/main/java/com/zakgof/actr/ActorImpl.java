package com.zakgof.actr;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class ActorImpl<T> implements ActorRef<T> {
	
	private volatile T object;
	private final ActorSystem actorSystem;
	private final IActorScheduler scheduler;
	private final String name;
	private final BiConsumer<T, Exception> exceptionHandler;
	
	ActorImpl(T object, Supplier<T> constructor, IActorScheduler scheduler, ActorSystem actorSystem, String name, BiConsumer<T, Exception> exceptionHandler) {
		this.actorSystem = actorSystem;
		this.exceptionHandler = exceptionHandler;
		this.name = name;
		if (object != null) {
			this.object = object;
		}
		this.scheduler = scheduler == null ? new DedicatedThreadScheduler() : scheduler;
		if (constructor != null) {
			ActorRef<?> current = Actr.current();
			Actr.setCurrent(this);
			this.object = constructor.get();
			Actr.setCurrent(current);
		}
		actorSystem.add(this);
	}

	@Override
	public void tell(Consumer<T> action) {
		ActorRef<?> caller = Actr.current();
		scheduleCall(action, caller);
	}

	private void scheduleCall(Consumer<T> action, ActorRef<?> caller) {
		scheduler.schedule(() -> {
			Actr.setCurrent(this);
			Actr.setCaller(caller);
			try {
				action.accept(object);
			} catch (Exception e) {
				exceptionHandler.accept(object, e);
			} finally {
				Actr.setCurrent(null);
				Actr.setCaller(null);
			}
		}, this);
	}

	@Override
	public void later(Consumer<T> action, long ms) {
		ActorRef<?> caller = Actr.current();
		actorSystem.later(() -> 
			scheduleCall(action, caller), ms
		);
	}
	
	@Override
	public <R> void ask(BiConsumer<T, Consumer<R>> action, Consumer<R> consumer) {
		tell(target -> action.accept(target, result -> Actr.caller().tell(c -> consumer.accept(result))));		
	}
	
	@Override
	public <R> void ask(Function<T, R> call, Consumer<R> consumer) {
		ask((target, callback) -> callback.accept(call.apply(target)), consumer);
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

	@Override
	public <C> ActorRef<C> actorOf(Supplier<C> constructor, String name) {
		return system().actorOf(constructor, this.name + "/" + name);
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (scheduler instanceof DedicatedThreadScheduler) {
			scheduler.destroy();
		}
	}

}