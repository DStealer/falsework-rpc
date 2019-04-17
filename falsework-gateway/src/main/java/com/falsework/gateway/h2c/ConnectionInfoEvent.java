package com.falsework.gateway.h2c;


import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;

public class ConnectionInfoEvent {
    private final String remoteHost;
    private final int remotePort ;
    private final ByteBuf bufData;
    public ConnectionInfoEvent(String remoteHost, int remotePort, ByteBuf bufData) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.bufData = bufData;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public ByteBuf getBufData() {
        return bufData;
    }
}
