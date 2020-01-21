package com.zakgof.actr.impl;

import java.util.Collection;

interface IRegSet<T> {

    interface IRegistration {
        void remove();
    }

    IRegistration add(T element);

    Collection<T> copy();
}
