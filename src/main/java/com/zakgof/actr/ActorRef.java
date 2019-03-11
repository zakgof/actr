package com.zakgof.actr;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActorRef<T> {
	
	private T object;
	private final Runnable shutdownInThread;
	private final Consumer<Runnable> asyncExec;
	private final BiConsumer<Runnable, Long> timerExec;
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final ActorSystem actorSystem;
	private final Runnable shutDownInCaller;
	
	public static <T> Builder<T> builder() {
		return new Builder<>(ActorSystem.dflt());
	}

	public static <T> Builder<T> builder(ActorSystem actorSystem) {
		return new Builder<T>(actorSystem);
	}

	public static <T> ActorRef<T> from(Supplier<T> constructor) {
		return ActorRef.<T>builder().constructor(constructor).build();
	}

	public static class Builder<T> {
		private ActorSystem actorSystem;
		private String threadName;
		private Consumer<Runnable> asyncExec;
		private BiConsumer<Runnable, Long> timerExec;
		private T object;
		private Supplier<T> constructor;
		private Consumer<T> destructor;
		private ScheduledExecutorService es;
		private Runnable postShutdown;

		public Builder(ActorSystem actorSystem) {
			this.actorSystem = actorSystem;
		}

		public Builder<T> threadName(String threadName) {
			this.threadName = threadName;
			return this;
		}
		
		public Builder<T> asyncExec(Consumer<Runnable> asyncExec) {
			this.asyncExec = asyncExec;
			return this;
		}
		
		public Builder<T> timerExec(BiConsumer<Runnable, Long> timerExec) {
			this.timerExec = timerExec;
			return this;
		}
		
		public Builder<T> object(T object) {
			this.object = object;
			return this;
		}
		
		public Builder<T> constructor(Supplier<T> constructor) {
			this.constructor = constructor;
			return this;
		}
		
		public Builder<T> destructor(Consumer<T> destructor) {
			this.destructor = destructor;
			return this;
		}
		
		public ActorRef<T> build() {
			if (constructor != null && object != null)
				throw new IllegalArgumentException("Not allowed to provide both object and constructor");
			if (constructor == null && object == null)
				throw new IllegalArgumentException("Provide either object or constructor");
			
			if (asyncExec == null) {
				es = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, threadName == null ? "actr" : threadName)); 
				this.postShutdown = () -> {
					es.shutdown();
					try {
						es.awaitTermination(10000, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
					}
				};
				asyncExec = es::submit;
				timerExec = (r, t) -> es.schedule(r, t, TimeUnit.MILLISECONDS);
			}
			ActorRef<T> actorRef = new ActorRef<T>(object, constructor, destructor, postShutdown, asyncExec, timerExec, actorSystem);
			return actorRef;
		}
	}
	
	public ActorRef(T object, Supplier<T> constructor, Consumer<T> destructor, Runnable postShutdown,
			Consumer<Runnable> asyncExec, BiConsumer<Runnable, Long> timerExec, ActorSystem actorSystem) {
		this.actorSystem = actorSystem;
		if (object != null) {
			this.object = object;
			actorSystem.add(this);
		}
		this.asyncExec = asyncExec;
		this.timerExec = timerExec;
		this.shutdownInThread = () -> {
			if (destructor != null) {
				destructor.accept(object);
			}
		};
		this.shutDownInCaller = postShutdown;
		if (constructor != null) {
			asyncExec.accept(() -> {
				actorSystem.setCurrentActor(this);
				this.object = constructor.get();
				actorSystem.add(this);
			});
		}
		
	}

	public void tell(Consumer<T> action) {
		if (!shutdown.get())
			asyncExec.accept(() -> {
				actorSystem.setCurrentActor(this);
				action.accept(object);
				actorSystem.setCurrentActor(null);
			});
	}
	
	public void later(Consumer<T> action, long ms) {
		if (!shutdown.get())
			timerExec.accept(() -> {
				actorSystem.setCurrentActor(this);
				action.accept(object);
				actorSystem.setCurrentActor(null);
			}, ms);
	}
	
	public <R> void ask(Function<T, R> call, Consumer<R> consumer) {
		ActorRef<?> caller = actorSystem.callerActor();
		if (!shutdown.get())
			asyncExec.accept(() -> {
				actorSystem.setCurrentActor(this);
				R result = call.apply(object);
				if (!shutdown.get()) {
					caller.asyncExec.accept(() -> consumer.accept(result));
				}
				actorSystem.setCurrentActor(null);
			});
	}

	public void destroy() {
		asyncExec.accept(shutdownInThread);
		shutdown.set(true);
		if (shutDownInCaller != null) {
			shutDownInCaller.run();
		}
	}

	T object() {
		return object;
	}
	
	@Override
	public String toString() {
		return "[Actor: " + object.getClass().getSimpleName() + "]";
	}

}