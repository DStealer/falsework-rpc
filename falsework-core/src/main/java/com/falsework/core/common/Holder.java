package com.falsework.core.common;

import com.google.common.base.Preconditions;

/**
 * 对象占位符
 *
 * @param <T>
 */
public class Holder<T> {
    private T t;

    public Holder(T t) {
        this.t = t;
    }

    public Holder() {
    }

    public T get() {
        return t;
    }

    public T getNonNull() {
        Preconditions.checkNotNull(t);
        return t;
    }

    public void set(T t) {
        this.t = t;
    }

    public void setNonNull(T t) {
        Preconditions.checkNotNull(t);
        this.t = t;
    }
}
