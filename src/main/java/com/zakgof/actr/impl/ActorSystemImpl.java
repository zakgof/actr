package com.zakgof.actr.impl;

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

import com.zakgof.actr.IActorBuilder;
import com.zakgof.actr.IActorRef;
import com.zakgof.actr.IActorScheduler;
import com.zakgof.actr.IActorSystem;
import com.zakgof.actr.IForkBuilder;
import com.zakgof.actr.Schedulers;

public class ActorSystemImpl implements IActorSystem {

    private static final int DEFAULT_FORKJOINSCHEDULER_THROUGHPUT = 10;

    private final IActorScheduler defaultScheduler;

    private final String name;
    private final IRegSet<ActorImpl<?>> actors = new FastRegSet<>();
    private final ScheduledExecutorService timer;

    private final CompletableFuture<String> terminator = new CompletableFuture<>();
    private final AtomicBoolean isShuttingDown = new AtomicBoolean();

    private volatile boolean isShutDown;

    public ActorSystemImpl(String name, IActorScheduler defaultScheduler) {
        this.name = name;
        this.defaultScheduler = defaultScheduler;
        this.timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "actr:" + name + ":timer");
            thread.setPriority(8);
            return thread;
        });
    }

    public ActorSystemImpl(String name) {
        this(name, Schedulers.newForkJoinPoolScheduler(DEFAULT_FORKJOINSCHEDULER_THROUGHPUT));
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
    @Override
    public CompletableFuture<String> shutdown() {
        if (isShuttingDown.compareAndSet(false, true)) {
            timer.execute(() -> {
                Collection<ActorImpl<?>> actorRefs = actors.copy();
                if (actorRefs.isEmpty()) {
                    internalShutdown();
                    return;
                }
                int[] actorsToGo = { actorRefs.size() };
                for (ActorImpl<?> actor : actorRefs) {
                    actor.dispose(() -> timer.execute(() -> {
                        actorsToGo[0]--;
                        if (actorsToGo[0] == 0) {
                            internalShutdown();
                        }
                    }));
                }
            });
        }
        return terminator;
    }

    private void internalShutdown() {
        timer.shutdownNow();
        defaultScheduler.close();
        isShutDown = true;
        terminator.complete("shutdown");
    }

    /**
     * @return a CompletableFuture to be triggered when actor system shutdown completes. The result value is the shutdown reason.
     */
    @Override
    public CompletableFuture<String> shutdownCompletable() {
        return terminator;
    }

    void add(ActorImpl<?> actorRef) {
        checkShutdown();
        actorRef.reg(actors.add(actorRef));
    }

    void remove(ActorImpl<?> actorRef) {
        actorRef.reg().remove();
    }

    private void checkShutdown() {
        if (isShuttingDown.get())
            throw new RuntimeException("Cannot add actor: actor system shutdown in progress");
        if (isShutDown)
            throw new RuntimeException("Cannot add actor: actor system is shut down");
    }

    /**
     * Get an instance of {@link ActorBuilderImpl} under this system
     *
     * @param <T> actor POJO class
     * @return ActorBuilder instance
     */
    @Override
    public <T> IActorBuilder<T> actorBuilder() {
        return new ActorBuilderImpl<>(this);
    }

    /**
     * Create a new actor under this system with a specified POJO instance factory and name.
     *
     * @param <T> actor POJO class
     * @param constructor factory to create actor POJO class instance
     * @param name actor name
     * @return ActorRef actor reference
     */
    @Override
    public <T> IActorRef<T> actorOf(Supplier<T> constructor, String name) {
        return this.<T> actorBuilder().constructor(constructor).name(name).build();
    }

    /**
     * Create a new actor under this system with a specified POJO instance factory and a autogenerated name.
     *
     * @param <T> actor POJO class
     * @param constructor factory to create actor POJO class instance.
     * @return ActorRef actor reference
     */
    @Override
    public <T> IActorRef<T> actorOf(Supplier<T> constructor) {
        return actorOf(constructor, Long.toHexString(new Random().nextLong()));
    }

    public static class ActorBuilderImpl<T> implements IActorBuilder<T> {
        private ActorSystemImpl actorSystem;
        private T object;
        private Supplier<T> constructor;
        private Consumer<T> destructor;
        private IActorScheduler scheduler;
        private String name;
        private BiConsumer<T, Exception> exceptionHandler;

        private ActorBuilderImpl(ActorSystemImpl actorSystem) {
            actorSystem.checkShutdown();
            this.actorSystem = actorSystem;
            this.scheduler = actorSystem.defaultScheduler;
            this.exceptionHandler = (obj, ex) -> ex.printStackTrace();
        }

        /**
         * Adds an existing actor POJO class instance to be used with the actor being constructed.
         *
         * @param object actor POJO class instance
         * @return this builder
         */
        @Override
        public IActorBuilder<T> object(T object) {
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
        @Override
        public IActorBuilder<T> constructor(Supplier<T> constructor) {
            this.constructor = constructor;
            return this;
        }

        /**
         * Adds a destructor to be called in actor thread context when the actor is being destroyed.
         *
         * @param destructor action to be called on actor destruction
         * @return this builder
         */
        @Override
        public IActorBuilder<T> destructor(Consumer<T> destructor) {
            this.destructor = destructor;
            return this;
        }

        /**
         * Sets a name for the actor being constructed.
         *
         * @param name actor name
         * @return this builder
         */
        @Override
        public IActorBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a scheduler for the actor being constructed.
         *
         * @param scheduler scheduler to be used for the actor being constructed
         * @return this builder
         */
        @Override
        public IActorBuilder<T> scheduler(IActorScheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * Sets an exception handler for the actor being constructed.
         *
         * Exception handler is triggered in actor's thread context whenever an exception occurs in actor's <i>ask</i>, <i>tell</i> or <i>later</i> call. Note that the exception handler is ignored when calling methods returning CallableFuture as in that case the exception is passed directly to the future.
         *
         * @param exceptionHandler exception handler to be triggered
         * @return this builder
         */
        @Override
        public IActorBuilder<T> exceptionHandler(BiConsumer<T, Exception> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Creates an actor using this builder.
         *
         * @return newly create ActorRef instance
         */
        @Override
        public IActorRef<T> build() {
            if (constructor != null && object != null)
                throw new IllegalArgumentException("Not allowed to provide both object and constructor");
            if (constructor == null && object == null)
                throw new IllegalArgumentException("Provide either object or constructor");

            return new ActorImpl<>(object, constructor, scheduler, actorSystem, name, exceptionHandler, destructor);
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

    @Override
    public <I, T> IForkBuilder<I, T> forkBuilder(Collection<I> ids) {
        return new ForkBuilderImpl<I, T>().ids(ids);
    }

    public interface TernaryConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    private class ForkBuilderImpl<I, T> implements IForkBuilder<I, T> {

        private Collection<I> ids;
        private Function<I, T> constructor;
        private Function<I, IActorScheduler> scheduler = i -> ActorSystemImpl.this.defaultScheduler;

        @Override
        public IForkBuilder<I, T> ids(Collection<I> ids) {
            this.ids = ids;
            return this;
        }

        @Override
        public IForkBuilder<I, T> constructor(Function<I, T> constructor) {
            this.constructor = constructor;
            return this;
        }

        @Override
        public IForkBuilder<I, T> scheduler(Function<I, IActorScheduler> scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        @Override
        public <R> void ask(BiFunction<I, T, R> action, Consumer<Map<I, R>> result) {
            ask((id, pojo, resultConsumer) -> resultConsumer.accept(action.apply(id, pojo)), result);
        }

        @Override
        public <R> void ask(TernaryConsumer<I, T, Consumer<R>> action, Consumer<Map<I, R>> result) {

            Map<I, R> map = new ConcurrentHashMap<>();
            for (I id : ids) {
                IActorRef<T> actor = ActorSystemImpl.this.<T>actorBuilder()
                    .constructor(() -> constructor.apply(id))
                    .scheduler(scheduler.apply(id))
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
