package com.falsework.core.mock;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.*;

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

    @Test
    public void tt03() {
        URI uri = URI.create("http://abc:8010/def?q=abc");
        Assert.assertEquals("http", uri.getScheme());
        Assert.assertEquals("abc:8010", uri.getAuthority());
        Assert.assertEquals("abc", uri.getHost());
        Assert.assertEquals("abc:8010", uri.getRawAuthority());
    }

    @Test
    public void tt04() {
        URI uri = URI.create("http://abc;abc:9090");
        Assert.assertEquals("abc;abc:9090",uri.getAuthority());
    }
}
