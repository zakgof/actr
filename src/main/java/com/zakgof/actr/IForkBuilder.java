package com.zakgof.actr;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.zakgof.actr.impl.ActorSystemImpl.TernaryConsumer;

public interface IForkBuilder<I, T> {

    <R> void ask(TernaryConsumer<I, T, Consumer<R>> action, Consumer<Map<I, R>> result);

    <R> void ask(BiFunction<I, T, R> action, Consumer<Map<I, R>> result);

    IForkBuilder<I, T> scheduler(Function<I, IActorScheduler> scheduler);

    IForkBuilder<I, T> constructor(Function<I, T> constructor);

    IForkBuilder<I, T> ids(Collection<I> ids);

}