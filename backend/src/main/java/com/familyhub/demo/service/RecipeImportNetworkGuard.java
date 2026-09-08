package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;

@Component
public class RecipeImportNetworkGuard {
    private static final String IMPORT_FAILURE_MESSAGE = "Could not import recipe.";

    public URI validatePublicHttpUri(String url) {
        try {
            return validatePublicHttpUri(new URI(url.trim()));
        } catch (URISyntaxException | RuntimeException ex) {
            throw importFailure();
        }
    }

    public URI validatePublicHttpUri(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw importFailure();
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw importFailure();
        }

        try {
            assertPublicHost(host);
        } catch (IOException ex) {
            throw importFailure();
        }
        return uri;
    }

    void assertPublicHost(String host) throws UnknownHostException {
        for (InetAddress address : InetAddress.getAllByName(host)) {
            assertPublicAddress(host, address);
        }
    }

    void assertPublicAddress(String host, InetAddress address) throws UnknownHostException {
        if (!isPublicAddress(address)) {
            throw new UnknownHostException("Host resolves to non-public address: " + host);
        }
    }

    boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return isPublicIpv4(bytes);
        }

        if (address instanceof Inet6Address) {
            if (isIpv4MappedIpv6(bytes)) {
                return isPublicIpv4(Arrays.copyOfRange(bytes, 12, 16));
            }
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            int third = bytes[2] & 0xff;
            int fourth = bytes[3] & 0xff;
            if ((first & 0xfe) == 0xfc) {
                return false;
            }
            return !(first == 0x20 && second == 0x01 && third == 0x0d && fourth == 0xb8);
        }

        return false;
    }

    private boolean isPublicIpv4(byte[] bytes) {
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        if (first == 0 || first == 10 || first == 127 || first >= 224) {
            return false;
        }
        if (first == 100 && second >= 64 && second <= 127) {
            return false;
        }
        if (first == 169 && second == 254) {
            return false;
        }
        if (first == 172 && second >= 16 && second <= 31) {
            return false;
        }
        if (first == 192 && (second == 0 || second == 168)) {
            return false;
        }
        if (first == 198 && (second == 18 || second == 19 || second == 51)) {
            return false;
        }
        return !(first == 203 && second == 0);
    }

    private boolean isIpv4MappedIpv6(byte[] bytes) {
        return bytes.length == 16
                && bytes[0] == 0
                && bytes[1] == 0
                && bytes[2] == 0
                && bytes[3] == 0
                && bytes[4] == 0
                && bytes[5] == 0
                && bytes[6] == 0
                && bytes[7] == 0
                && bytes[8] == 0
                && bytes[9] == 0
                && (bytes[10] & 0xff) == 0xff
                && (bytes[11] & 0xff) == 0xff;
    }

    private static BadRequestException importFailure() {
        return new BadRequestException(IMPORT_FAILURE_MESSAGE);
    }
}
