package com.zakgof.actr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActorSystem {
	
	private final IActorScheduler scheduler = new ForkJoinPoolScheduler(10);

	private static final ActorSystem DEFAULT = new ActorSystem("default");
	private String name;
	private Map<Object, ActorImpl<?>> actors = new ConcurrentHashMap<>();
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "actr:" + name + ":timer");
		thread.setPriority(8);
		return thread;
	});

	private CompletableFuture<String> terminator = new CompletableFuture<>();

	private AtomicBoolean isShuttingDown = new AtomicBoolean();
	private volatile boolean isShutDown;
	

	public ActorSystem(String name) {
		this.name = name;
	}

	public static ActorSystem dflt() {
		return DEFAULT;
	}

	public static ActorSystem create(String name) {
		return new ActorSystem(name);
	}

	public CompletableFuture<String> shutdown() {
		if (this == DEFAULT) {
			throw new RuntimeException("Cannot terminate default actor system");
		}
		if (isShuttingDown.compareAndSet(false, true)) {
			timer.execute(() -> {
				Collection<ActorImpl<?>> actorRefs = new ArrayList<>(actors.values());
				int[] actorsToGo = {actorRefs.size()};
				for(ActorImpl<?> actor : actorRefs) {
					actor.dispose(() ->  timer.execute(() -> {
						actorsToGo[0]--;
						if (actorsToGo[0] == 0) {
							scheduler.destroy();
							timer.shutdownNow();
							isShutDown = true;
							terminator.complete("shutdown");
						}
					}));
				}
			});
		}
		return terminator;
	}
	
	public CompletableFuture<String> shutdownCompletable() {
		return terminator;
	}

	void add(ActorImpl<?> actorRef) {
		checkShutdown();
		actors.put(actorRef.object(), actorRef);
	}
	

	void remove(ActorImpl<?> actorRef) {
		actors.remove(actorRef.object());
	}

	private void checkShutdown() {
		if (isShuttingDown.get())
			throw new RuntimeException("Cannot add actor: actor system shutdown in progress");
		if (isShutDown)
			throw new RuntimeException("Cannot add actor: actor system is shut down");
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
		private IActorScheduler scheduler;
		private boolean owningScheduler;
		private String name;
		private BiConsumer<T, Exception> exceptionHandler;

		public ActorBuilder(ActorSystem actorSystem) {
			actorSystem.checkShutdown();
			this.actorSystem = actorSystem;
			this.scheduler = actorSystem.scheduler;
			this.exceptionHandler = (obj, ex) -> ex.printStackTrace();
		}
		
		public ActorBuilder<T> object(T object) {
			this.object = object;
			return this;
		}
		
		public ActorBuilder<T> constructor(Supplier<T> constructor) {
			this.constructor = constructor;
			return this;
		}
		
		public ActorBuilder<T> destructor(Consumer<T> destructor) {
			this.destructor = destructor;
			return this;
		}

		public ActorBuilder<T> name(String name) {
			this.name = name;
			return this;
		}
		
		public ActorBuilder<T> scheduler(IActorScheduler scheduler, boolean owning) {
			this.scheduler = scheduler;
			this.owningScheduler = owning;
			return this;
		}
		
		public ActorBuilder<T> exceptionHandler(BiConsumer<T, Exception> exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
			return this;
		}
		
		public ActorRef<T> build() {
			if (constructor != null && object != null)
				throw new IllegalArgumentException("Not allowed to provide both object and constructor");
			if (constructor == null && object == null)
				throw new IllegalArgumentException("Provide either object or constructor");
			
			ActorRef<T> actorRef = new ActorImpl<T>(object, constructor, scheduler, owningScheduler, actorSystem, name, exceptionHandler, destructor);
			return actorRef;
		}

	}

	void later(Runnable runnable, long ms) {
		if (!timer.isShutdown() && !timer.isTerminated()) {
			timer.schedule(runnable, ms, TimeUnit.MILLISECONDS);
		}
	}

	public <I, T> ForkBuilder<I, T> forkBuilder() {
		return new ForkBuilder<>();
	}

	public interface TernaryConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}
	
	public class ForkBuilder<I, T> {

		private List<I> ids;
		private Function<I, T> constructor;

		public ForkBuilder<I, T> ids(@SuppressWarnings("unchecked") I... ids) {
			this.ids = Arrays.asList(ids);
			return this;
		}
		
		public ForkBuilder<I, T> constructor(Function<I, T> constructor) {
			this.constructor = constructor;
			return this;
		}
		
		public <R> void ask(TernaryConsumer<I, T, Consumer<R>> action, Consumer<Map<I, R>> result) {
			
			Map<I, R> map = new ConcurrentHashMap<>();
			for (I id : ids) {
				ActorRef<T> actor = actorOf(() ->  constructor.apply(id));
				Consumer<R> callback = r -> {
					map.put(id, r);
					if (map.size() == ids.size()) {
						result.accept(map);
					}
				};
				actor.ask((target, c) -> action.accept(id, target, c) , callback);
			}
		}
	}

}
