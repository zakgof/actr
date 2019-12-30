package com.zakgof.actr;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface for addressing actors.
 *
 * @param <T> actor POJO class
 */
public interface ActorRef<T> extends AutoCloseable {

    /**
     * @return actor's @link {ActorSystem}
     */
    ActorSystem system();

    /**
     * Sends a message to the actor defined by this reference.
     *
     * The specified action is executed on the actor's object asynchronously in actor's thread context. This method does not wait for completion of the action, it returns immediately.
     *
     * @param action action to be executed on actor's object.
     */
    void tell(Consumer<T> action);

    /**
     * Schedules an action to be executed once after a specified time.
     *
     * The specified action is executed on the actor's object asynchronously in actor's thread context.
     *
     * @param action action to be executed on actor's object.
     * @param ms delay in milliseconds
     */
    void later(Consumer<T> action, long ms);

    /**
     * Sends a message to actor and gets a response.
     *
     * Performs the specified call on the actor's object asynchronously. The call is executed in this actor's thread context, the return value from the action is then passed to the consumer in the caller's actor thread context. If the method is
     * called not from actor's context, exception is thrown.
     *
     * This method does not wait for response, it returns immediately.
     *
     * @param <R> actor call response class
     * @param action action to be executed on actor's object, return value will be the response
     * @param consumer consumer to receive the response
     */
    <R> void ask(Function<T, R> action, Consumer<R> consumer);

    /**
     * Sends a message to actor and gets a response.
     *
     * Performs the specified call on the actor's object asynchronously. The call is executed in this actor's thread context. The action get a response consumer as an additional parameter, it should pass the result to that consumer. The response in
     * passed to the caller's consumer in the caller's actor thread context. If the method is called not from actor's context, exception is thrown.
     *
     * This method does not wait for response, it returns immediately.
     *
     * @param <R> actor call response class
     * @param action action to be executed on actor's object
     * @param consumer consumer to receive the response
     */
    <R> void ask(BiConsumer<T, Consumer<R>> action, Consumer<R> consumer);

    /**
     * Destroy the actor.
     *
     * If defined, destructor will be called in actor's thread context.
     *
     * If actor had a dedicated scheduler, the scheduler will be destroyed as well.
     *
     * Messages pending for this actor will be processed before terminating (TODO ??)
     */
    @Override
	void close();

}
