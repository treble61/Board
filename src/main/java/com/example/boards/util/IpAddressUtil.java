package com.example.boards.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting client IP addresses from HTTP requests
 * Handles proxy headers and direct connections
 */
public class IpAddressUtil {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * Extract the real client IP address from the HTTP request
     *
     * Checks proxy headers first (X-Forwarded-For, etc.) to get the original client IP
     * Falls back to remote address if no proxy headers are present
     *
     * @param request The HTTP servlet request
     * @return The client's IP address, or "unknown" if it cannot be determined
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Check proxy headers first
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
                // The first one is the original client
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Check if an IP address is valid IPv4 or IPv6
     *
     * @param ip The IP address string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        // Basic IPv4 validation
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (ip.matches(ipv4Pattern)) {
            return true;
        }

        // Basic IPv6 validation (simplified)
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        if (ip.matches(ipv6Pattern)) {
            return true;
        }

        // IPv6 compressed format
        if (ip.contains("::")) {
            return true;
        }

        return false;
    }

    /**
     * Normalize IP address for rate limiting
     * Ensures consistent format for caching and tracking
     *
     * @param ip The IP address to normalize
     * @return Normalized IP address
     */
    public static String normalizeIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }

        // Remove whitespace
        ip = ip.trim();

        // Convert localhost variations to standard form
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }
}
