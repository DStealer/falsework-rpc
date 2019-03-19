package com.falsework.core.common;

public abstract class Utils {
    /**
     * 传播异常
     *
     * @param throwable
     */
    public static void propagateExeception(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }

        throw new RuntimeException("propagate as need", throwable);
    }
}
