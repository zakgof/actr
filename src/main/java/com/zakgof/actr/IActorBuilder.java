package com.zakgof.actr;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IActorBuilder<T> {

    /**
     * Adds an existing actor POJO class instance to be used with the actor being constructed.
     *
     * @param object actor POJO class instance
     * @return this builder
     */
    IActorBuilder<T> object(T object);

    /**
     * Adds a factory for POJO class instance creation to be used with the actor being constructed.
     *
     * Constructor will be called during {@link #build()} call in a synchronous manner
     *
     * @param constructor POJO class instance factory
     * @return this builder
     */
    IActorBuilder<T> constructor(Supplier<T> constructor);

    /**
     * Adds a destructor to be called in actor thread context when the actor is being destroyed.
     *
     * @param destructor action to be called on actor destruction
     * @return this builder
     */
    IActorBuilder<T> destructor(Consumer<T> destructor);

    /**
     * Sets a name for the actor being constructed.
     *
     * @param name actor name
     * @return this builder
     */
    IActorBuilder<T> name(String name);

    /**
     * Sets a scheduler for the actor being constructed.
     *
     * @param scheduler scheduler to be used for the actor being constructed
     * @return this builder
     */
    IActorBuilder<T> scheduler(IActorScheduler scheduler);

    /**
     * Sets an exception handler for the actor being constructed.
     *
     * Exception handler is triggered in actor's thread context whenever an exception occurs in actor's <i>ask</i>, <i>tell</i> or <i>later</i> call. Note that the exception handler is ignored when calling methods returning CallableFuture as in that
     * case the exception is passed directly to the future.
     *
     * @param exceptionHandler exception handler to be triggered
     * @return this builder
     */
    IActorBuilder<T> exceptionHandler(BiConsumer<T, Exception> exceptionHandler);

    /**
     * Creates an actor using this builder.
     *
     * @return newly create ActorRef instance
     */
    IActorRef<T> build();

}