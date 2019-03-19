package com.falsework.core.server;

import java.util.LinkedList;
import java.util.List;

public class CompositeServerRegister implements ServerRegister {
    private final List<ServerRegister> registerList = new LinkedList<>();

    public void addServerRegister(ServerRegister register) {
        this.registerList.add(register);
    }

    public void removeServerRegister(ServerRegister register) {
        this.registerList.remove(register);
    }

    @Override
    public void register() throws Exception {
        for (ServerRegister register : this.registerList) {
            register.register();
        }
    }

    @Override
    public void unregister() throws Exception {
        for (ServerRegister register : this.registerList) {
            register.unregister();
        }
    }
}
