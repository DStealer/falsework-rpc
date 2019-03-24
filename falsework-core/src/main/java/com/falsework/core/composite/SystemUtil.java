/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsework.core.composite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;

public enum SystemUtil {

    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemUtil.class);

    /**
     * 获取本地非回环网卡
     *
     * @return
     */
    public static String serverIPv4() {
        String candidateAddress = null;
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                Enumeration<InetAddress> inetAddresses = nic.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    String address = inetAddresses.nextElement().getHostAddress();
                    String nicName = nic.getName();
                    if (nicName.startsWith("eth0") || nicName.startsWith("en0")) {
                        return address;
                    }
                    if (nicName.endsWith("0") || candidateAddress == null) {
                        candidateAddress = address;
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Cannot resolve local network address", e);
        }
        return candidateAddress == null ? "127.0.0.1" : candidateAddress;
    }

    /**
     * 验证ip和端口
     *
     * @param ip
     * @param port
     */
    public static void validate(String ip, int port) {
        try {
            InetSocketAddress address = new InetSocketAddress(ip, port);
            NetworkInterface anInterface = NetworkInterface.getByInetAddress(address.getAddress());
            if (anInterface == null) {
                LOGGER.error("Can't binding {}:{} to any local network interface", ip, port);
                throw new IllegalArgumentException("not valid ip or port");
            }
            if (anInterface.isLoopback()) {
                LOGGER.warn("Binging loop back interface only available in development");
            }
            LOGGER.info("Bing service on interface: {} with {}:{}", anInterface.getName(), ip, port);
        } catch (SocketException e) {
            throw new RuntimeException("Cannot resolve local network address", e);
        }
    }

    /**
     * 获取本机主键名称
     *
     * @return
     */
    public static String hostname() {
        try {
           return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.warn("can't find hostname,return unknown");
        }
        return "UNKNOWN";
    }
}
