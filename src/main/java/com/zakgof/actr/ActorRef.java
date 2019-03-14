package com.zakgof.actr;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ActorRef<T> {

	ActorSystem system();

	<R> void ask(Function<T, R> call, Consumer<R> consumer);

	void tell(Consumer<T> action);
	
	
	
	// void later(Consumer<T> action, long ms);

}
