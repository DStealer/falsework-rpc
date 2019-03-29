package com.falsework.core.common;

import com.google.common.base.Preconditions;

/**
 * 对象占位符
 *
 * @param <T>
 */
public class Holder<T> {
    private T t;
    private boolean set;

    public Holder(T t) {
        this.set = true;
        this.t = t;
    }

    public Holder() {
        this.set = false;
    }

    public T get() {
        return t;
    }

    public T getNonNull() {
        Preconditions.checkNotNull(t);
        return t;
    }

    public void set(T t) {
        this.set=true;
        this.t = t;
    }

    public void setNonNull(T t) {
        Preconditions.checkNotNull(t);
        this.set=true;
        this.t = t;
    }

    public boolean isSet() {
        return set;
    }
}
