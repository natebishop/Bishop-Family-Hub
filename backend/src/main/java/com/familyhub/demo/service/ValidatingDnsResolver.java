package com.familyhub.demo.service;

import org.apache.hc.client5.http.DnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

class ValidatingDnsResolver implements DnsResolver {

    @FunctionalInterface
    interface HostAddressResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final HostAddressResolver delegate;
    private final RecipeImportNetworkGuard networkGuard;

    ValidatingDnsResolver(HostAddressResolver delegate, RecipeImportNetworkGuard networkGuard) {
        this.delegate = delegate;
        this.networkGuard = networkGuard;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = delegate.resolve(host);
        for (InetAddress address : addresses) {
            networkGuard.assertPublicAddress(host, address);
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) {
        return host;
    }
}
