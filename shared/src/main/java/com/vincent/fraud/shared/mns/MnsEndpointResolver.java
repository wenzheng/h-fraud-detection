package com.vincent.fraud.shared.mns;

public final class MnsEndpointResolver {

    private MnsEndpointResolver() {
    }

    public static String resolve(String endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("MNS endpoint must not be null");
        }

        String normalized = endpoint.trim();
        if (normalized.length() >= 2
                && normalized.startsWith("\"")
                && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("MNS endpoint must not be blank");
        }

        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        return normalized;
    }
}
