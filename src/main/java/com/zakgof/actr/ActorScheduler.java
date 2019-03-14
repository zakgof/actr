package com.zakgof.actr;

public interface ActorScheduler {
	
	void schedule(Runnable task, Object actorId);
	
	void destroy(); // TODO: await ? terminate ?
}
