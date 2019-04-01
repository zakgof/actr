package com.zakgof.actr;

public interface IActorScheduler {
	
	void schedule(Runnable task, Object actorId);
	
	default void destroy() {
	};
}
