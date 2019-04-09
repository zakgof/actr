package com.zakgof.actr;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ActorRef<T> {

	/** 
	 * @return actor's @link {ActorSystem} 
	 */
	ActorSystem system();
	

	/**
	 * Sends a message to the actor defined by this reference.
	 * 
	 * The specified action is executed on the actor's object asynchronously in actor's thread context.
	 * This method does not wait for completion of the action, it returns immediately.
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
	 * Sends a message to actor and get a response.
	 * 
	 * Performs the specified call on the actor's object asynchronously. The call is executed in the actor's thread context, the response consumer is called in the caller's actor thread context.
	 * If the method is called not from actor's context, exception is thrown.
	 * This method does not wait for response, it returns immediately.
	 * 
	 * @param call action to be executed on actor's object 
	 * @param consumer consumer to receive the response 
	 *  
	 */
	<R> void ask(Function<T, R> call, Consumer<R> consumer);
	
	<R> void ask(BiConsumer<T, Consumer<R>> action, Consumer<R> consumer);
	
	

	public <C> ActorRef<C> actorOf(Supplier<C> constructor, String name);
	

}
