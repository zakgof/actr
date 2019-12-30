package com.zakgof.actr;

public interface IActorScheduler extends AutoCloseable {

    void schedule(Runnable task, Object actorId);

    @Override
	default void close() {
    };
}
