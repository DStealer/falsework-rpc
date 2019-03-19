package com.falsework.core.aop.common;

import com.falsework.core.common.Props;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;

import java.util.Map;

/**
 * 环境感知
 */
public abstract class EnvAwareModule extends AbstractModule {
    private Props props;
    private Map<String, Object> global;
    private boolean sealed;


    protected final Props getProps() {
        Preconditions.checkNotNull(this.props, "props is null");
        return props;
    }

    final EnvAwareModule setProps(Props props) {
        Preconditions.checkNotNull(props, "props is null");
        Preconditions.checkState(!this.sealed, "env sealed");
        this.props = props;
        return this;
    }

    @SuppressWarnings("unchecked")
    protected final <T> T getGlobal(String key) {
        Preconditions.checkNotNull(this.global, "global is null");
        return (T) this.global.get(key);
    }

    final EnvAwareModule setGlobal(Map<String, Object> global) {
        Preconditions.checkNotNull(global, "global is null");
        Preconditions.checkState(!this.sealed, "env sealed");
        this.global = global;
        return this;
    }

    final void sealed() {
        this.sealed = true;
    }
}
