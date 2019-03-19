package com.falsework.core.composite;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopStreamObserver<T> implements StreamObserver<T> {
    private final Logger logger;

    public NoopStreamObserver() {
        this.logger = LoggerFactory.getLogger(NoopStreamObserver.class);
    }

    public NoopStreamObserver(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onNext(T value) {

    }

    @Override
    public void onError(Throwable t) {
        logger.error("Stream observer error", t);
    }

    @Override
    public void onCompleted() {

    }
}
