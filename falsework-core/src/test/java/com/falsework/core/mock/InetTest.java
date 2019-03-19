package com.falsework.core.mock;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;

public class InetTest {

    @Test
    public void tt01() throws SocketException {
        InetSocketAddress address = new InetSocketAddress("192.168.105.1", 8080);
        NetworkInterface anInterface = NetworkInterface.getByInetAddress(address.getAddress());
        System.out.println(anInterface);
    }

    @Test
    public void tt02() throws IOException {
        InetSocketAddress address = new InetSocketAddress("192.168.105.1", 8080);
        NetworkInterface anInterface = NetworkInterface.getByInetAddress(address.getAddress());
        System.out.println(anInterface);
    }
}
