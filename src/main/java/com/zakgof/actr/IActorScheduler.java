package com.zakgof.actr;

public interface IActorScheduler {
	
	void schedule(Runnable task, Object actorId);
	
	void destroy(); // TODO: await ? terminate ?
}
