package com.zakgof.actr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActorSystem {

    private static final int DEFAULT_FORKJOINSCHEDULER_THROUGHPUT = 10;

    private final IActorScheduler scheduler;

    private final String name;
    private final Map<Object, ActorImpl<?>> actors = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timer;

    private final CompletableFuture<String> terminator = new CompletableFuture<>();
    private final AtomicBoolean isShuttingDown = new AtomicBoolean();

    private volatile boolean isShutDown;

    /**
     * Create a new actor system with the specified name
     *
     * @param name actor system name
     * @return newly created actor system
     */
    public static ActorSystem create(String name) {
        return new ActorSystem(name, new ForkJoinPoolScheduler(DEFAULT_FORKJOINSCHEDULER_THROUGHPUT));
    }

    /**
     * Create a new actor system with the specified name
     *
     * @param name actor system name
     * @param scheduler default scheduler for new actors
     * @return newly created actor system
     */
    public static ActorSystem create(String name, IActorScheduler scheduler) {
        return new ActorSystem(name, scheduler);
    }

    private ActorSystem(String name, IActorScheduler scheduler) {
        this.name = name;
        this.scheduler = scheduler;
        this.timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "actr:" + name + ":timer");
            thread.setPriority(8);
            return thread;
        });
    }

    /**
     * Initiate an orderly shutdown of the actor system.
     *
     * The currently running actor actions are not terminated; new calls to actors are ignored, actor system triggers actor destructors in their respective thread contexts.
     *
     * Creating new actors under this system will fail after initiating the shutdown.
     *
     * Clients may use {@link shutdownCompletable} to be notified when the shutdown procedure completes.
     *
     * @return CompletableFuture that client may use to be notified when shutdown completes; the supplied string is shutdown reason
     */
    public CompletableFuture<String> shutdown() {
        if (isShuttingDown.compareAndSet(false, true)) {
            timer.execute(() -> {
                Collection<ActorImpl<?>> actorRefs = new ArrayList<>(actors.values());
                int[] actorsToGo = { actorRefs.size() };
                for (ActorImpl<?> actor : actorRefs) {
                    actor.dispose(() -> timer.execute(() -> {
                        actorsToGo[0]--;
                        if (actorsToGo[0] == 0) {
                            scheduler.close();
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

    /**
     * @return a CompletableFuture to be triggered when actor system shutdown completes. The result value is the shutdown reason.
     */
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

    /**
     * Get an instance of {@link ActorBuilder} under this system
     *
     * @param <T> actor POJO class
     * @return ActorBuilder instance
     */
    public <T> ActorBuilder<T> actorBuilder() {
        return new ActorBuilder<T>(this);
    }

    /**
     * Create a new actor under this system with a specified POJO instance factory and name.
     *
     * @param <T> actor POJO class
     * @param constructor factory to create actor POJO class instance
     * @param name actor name
     * @return ActorRef actor reference
     */
    public <T> ActorRef<T> actorOf(Supplier<T> constructor, String name) {
        return this.<T> actorBuilder().constructor(constructor).name(name).build();
    }

    /**
     * Create a new actor under this system with a specified POJO instance factory and a autogenerated name.
     *
     * @param <T> actor POJO class
     * @param constructor factory to create actor POJO class instance.
     * @return ActorRef actor reference
     */
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

        private ActorBuilder(ActorSystem actorSystem) {
            actorSystem.checkShutdown();
            this.actorSystem = actorSystem;
            this.scheduler = actorSystem.scheduler;
            this.exceptionHandler = (obj, ex) -> ex.printStackTrace();
        }

        /**
         * Adds an existing actor POJO class instance to be used with the actor being constructed.
         *
         * @param object actor POJO class instance
         * @return this builder
         */
        public ActorBuilder<T> object(T object) {
            this.object = object;
            return this;
        }

        /**
         * Adds a factory for POJO class instance creation to be used with the actor being constructed.
         *
         * Constructor will be called during {@link #build()} call in a synchronous manner
         *
         * @param constructor POJO class instance factory
         * @return this builder
         */
        public ActorBuilder<T> constructor(Supplier<T> constructor) {
            this.constructor = constructor;
            return this;
        }

        /**
         * Adds a destructor to be called in actor thread context when the actor is being destroyed.
         *
         * @param destructor action to be called on actor destruction
         * @return this builder
         */
        public ActorBuilder<T> destructor(Consumer<T> destructor) {
            this.destructor = destructor;
            return this;
        }

        /**
         * Sets a name for the actor being constructed.
         *
         * @param name actor name
         * @return this builder
         */
        public ActorBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a scheduler for the actor being constructed.
         *
         * @param scheduler scheduler to be used for the actor being constructed
         * @param owning set {@code true} if the actor should own the scheduler and destroy it when the actor shuts down.
         * @return this builder
         */
        public ActorBuilder<T> scheduler(IActorScheduler scheduler, boolean owning) {
            this.scheduler = scheduler;
            this.owningScheduler = owning;
            return this;
        }

        /**
         * Sets an exception handler for the actor being constructed.
         *
         * @param exceptionHandler exception handler to be triggered whenever an exception occurs in any actor call.
         * @return this builder
         */
        public ActorBuilder<T> exceptionHandler(BiConsumer<T, Exception> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Creates an actor using this builder.
         *
         * @return newly create ActorRef instance
         */
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

    @Override
    public String toString() {
        return "ActorSystem " + name;
    }

    public <T> void executeAsActor(T instance, Consumer<ActorRef<T>> call) {
        BlockingThreadScheduler blockingThreadScheduler = new BlockingThreadScheduler();
        ActorRef<T> actor = this.<T>actorBuilder().object(instance).scheduler(blockingThreadScheduler, false).build();
        actor.tell(obj -> call.accept(actor));
        blockingThreadScheduler.start();
    }

    public <I, T> ForkBuilder<I, T> forkBuilder(Collection<I> ids) {
        return new ForkBuilder<I, T>().ids(ids);
    }

    public interface TernaryConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    public class ForkBuilder<I, T> {

        private Collection<I> ids;
        private Function<I, T> constructor;
        private Function<I, IActorScheduler> scheduler = $ -> ActorSystem.this.scheduler;
        private boolean owningScheduler = false;

        public ForkBuilder<I, T> ids(Collection<I> ids) {
            this.ids = ids;
            return this;
        }

        public ForkBuilder<I, T> constructor(Function<I, T> constructor) {
            this.constructor = constructor;
            return this;
        }

        public ForkBuilder<I, T> scheduler(Function<I, IActorScheduler> scheduler, boolean owningScheduler) {
            this.scheduler = scheduler;
            this.owningScheduler = owningScheduler;
            return this;
        }

        public <R> void ask(BiFunction<I, T, R> action, Consumer<Map<I, R>> result) {
            ask((id, pojo, resultConsumer) -> resultConsumer.accept(action.apply(id, pojo)), result);
        }

        public <R> void ask(TernaryConsumer<I, T, Consumer<R>> action, Consumer<Map<I, R>> result) {

            Map<I, R> map = new ConcurrentHashMap<>();
            for (I id : ids) {
                ActorRef<T> actor = ActorSystem.this.<T>actorBuilder()
                    .constructor(() -> constructor.apply(id))
                    .scheduler(scheduler.apply(id), owningScheduler)
                    .build();
                Consumer<R> callback = r -> {
                    map.put(id, r);
                    if (map.size() == ids.size()) {
                        result.accept(map);
                    }
                };
                actor.ask((target, c) -> action.accept(id, target, c), callback);
            }
        }
    }

}
