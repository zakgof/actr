package com.zakgof.actr;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ActorRef<T> {

	ActorSystem system();

	<R> void ask(Function<T, R> call, Consumer<R> consumer);
	
	<R> void ask(BiConsumer<T, Consumer<R>> action, Consumer<R> consumer);

	void tell(Consumer<T> action);
	
	void later(Consumer<T> action, long ms);

	public <C> ActorRef<C> actorOf(Supplier<C> constructor, String name);
	

}
