package com.zakgof.actr;

import java.util.concurrent.CompletableFuture;
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
    private final Consumer<T> destructor;
	private volatile Object box;

    ActorImpl(T object, Supplier<T> constructor, IActorScheduler scheduler, ActorSystem actorSystem, String name, BiConsumer<T, Exception> exceptionHandler, Consumer<T> destructor) {
        this.actorSystem = actorSystem;
        this.exceptionHandler = exceptionHandler;
        this.name = name;
        this.destructor = destructor;
        if (object != null) {
            this.object = object;
        }
        this.scheduler = scheduler;
        scheduler.actorCreated(this);
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
       scheduleCallErrorAware(action, caller, e -> exceptionHandler.accept(object, e));
    }

    private void scheduleCallErrorAware(Consumer<T> action, ActorRef<?> caller, Consumer<Exception> exceptionCallback) {
    	scheduler.schedule(() -> {
            Actr.setCurrent(this);
            Actr.setCaller(caller);
            try {
                if (object == null)
                    return;
                action.accept(object);
            } catch (Exception e) {
                exceptionCallback.accept(e);
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
        ActorRef<?> caller = safeCaller();
        Consumer<R> completion = result -> caller.tell(c -> consumer.accept(result));
        tell(target -> action.accept(target, completion));
    }

    @Override
    public <R> void ask(Function<T, R> call, Consumer<R> consumer) {
        ask((target, callback) -> callback.accept(call.apply(target)), consumer);
    }

    @Override
    public <R> CompletableFuture<R> ask(BiConsumer<T, Consumer<R>> action) {
    	ActorRef<?> caller = safeCaller();
    	CompletableFuture<R> future = new CompletableFuture<>();
    	Consumer<R> completion = result -> caller.tell(c -> future.complete(result));
    	Consumer<Exception> failure = exception -> caller.tell(c -> future.completeExceptionally(exception));
    	scheduleCallErrorAware(target -> action.accept(target, completion), caller, failure);
    	return future;
    }

    @Override
    public <R> CompletableFuture<R> ask(Function<T, R> action) {
    	return ask((target, callback) -> callback.accept(action.apply(target)));
    }

	private static ActorRef<?> safeCaller() {
		ActorRef<?> caller = Actr.current();
        if (caller == null)
            throw new IllegalStateException("It is not allowed to call ask from non-actor context. There's no actor to receive the response");
		return caller;
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

    /** Called internally from system */
    void dispose(Runnable whenFinished) {
        tell(o -> {
            if (destructor != null) {
                try {
                    destructor.accept(object);
                } catch (Exception ex) {
                    ex.printStackTrace(); // TODO: logging
                }
            }
            system().remove(this);
            scheduler.actorDisposed(this);
            object = null;
            whenFinished.run();
        });

    }

    @Override
    public void close() {
        dispose(() -> {});
    }

	void box(Object box) {
		this.box = box;
	}

	Object box() {
		return box;
	}

}