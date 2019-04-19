package com.zakgof.actr;

/**
 * Helper class to work with actor contexts
 */
public class Actr {
	
	private static ThreadLocal<ActorRef<?>> currentActor = new ThreadLocal<>();
	private static ThreadLocal<ActorRef<?>> callerActor = new ThreadLocal<>();
	
	/**
	 * Gets the reference to the actor from its code.
	 * 
	 * When called from a properly called actor action, return this actor's ActorRef.  
	 * 
	 * Returns null if called not from from actor context
	 * 
	 * @return {@link ActorRef} for the actor being called
	 */
	@SuppressWarnings("unchecked")
	public static <T> ActorRef<T> current() {
		return (ActorRef<T>)currentActor.get();
	}
	
	/**
	 * Gets the reference to the actor calling this actor.
	 * 
	 * When called from a properly called actor tell/ask/later action, return the actor from which context this actor's action was called.  
	 * 
	 * If called in a callback for {@link ActorRef#ask} calls, this method returns a reference to the 'asked' actor.
	 * 
	 * For ask/later calls not from actor context, this method returns null
	 *  
	 * @return {@link ActorRef} for the caller actor, or null if called not from actor context or from an actor called from outside any actor context
	 */
	@SuppressWarnings("unchecked")
	public static <T> ActorRef<T> caller() {
		return (ActorRef<T>)callerActor.get();
	}
	
	/**
	 * Returns the current actor's system.
	 * @return current actor system or null if called not from actor context
	 */
	public static ActorSystem system() {
		ActorRef<?> actor = current();
		return actor == null ? null : current().system();
	}
	
	static void setCurrent(ActorRef<?> actor) {
		currentActor.set(actor);
	}
	
	static void setCaller(ActorRef<?> actor) {
		callerActor.set(actor);
	}

}
