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
	private final boolean owningScheduler;
	private final Consumer<T> destructor;
	
	ActorImpl(T object, Supplier<T> constructor, IActorScheduler scheduler, boolean owningScheduler, ActorSystem actorSystem, String name, BiConsumer<T, Exception> exceptionHandler, Consumer<T> destructor) {
		this.actorSystem = actorSystem;
		this.exceptionHandler = exceptionHandler;
		this.name = name;
		this.owningScheduler = owningScheduler;
		this.destructor = destructor;
		if (object != null) {
			this.object = object;
		}
		this.scheduler = scheduler == null ? new DedicatedThreadScheduler() : scheduler;
		if (constructor != null) {
			this.object = constructor.get();
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
				if (object == null)
					return;
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
		actorSystem.later(() -> {
			if (object != null) {
				scheduleCall(action, caller);
			}
		}, ms);
	}
	
	@Override
	public <R> void ask(BiConsumer<T, Consumer<R>> action, Consumer<R> consumer) {
		ActorRef<?> caller = Actr.current();
		if (caller == null)
			throw new IllegalStateException("It is not allowed to call ask from non-actor context. There's no actor to receive the response");
		tell(target -> action.accept(target, result -> {
			caller.tell(c -> {
				consumer.accept(result);	
			});	
		}));		
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
		return "[Actor: " + (name == null ? (object == null ? "<disposed>" : object.getClass().getSimpleName()) : name) + "]";
	}

	@Override
	public ActorSystem system() {
		return actorSystem;
	}

	@Override
	public <C> ActorRef<C> actorOf(Supplier<C> constructor, String name) {
		return system().actorOf(constructor, this.name + "/" + name);
	}

	public void dispose(Runnable whenFinished) {
		tell(o -> {
			if (destructor != null) {
				try {
					destructor.accept(object);
				} catch (Exception ex) {
					ex.printStackTrace(); // TODO: logging
				}
			}
			system().remove(this);
			if (owningScheduler) {
				scheduler.destroy();
			}
			object = null;
			whenFinished.run();
		});
		
	}

}