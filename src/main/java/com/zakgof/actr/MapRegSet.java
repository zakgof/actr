package com.zakgof.actr;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapRegSet<T> implements IRegSet<T> {

    private final Map<T, T> map = new ConcurrentHashMap<>();

    @Override
    public IRegistration add(T element) {
        map.put(element, element);
        return () -> map.remove(element);
    }

    @Override
    public Collection<T> copy() {
        return map.keySet();
    }

}
