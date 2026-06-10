package com.vincent.fraud.shared.mns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MnsEndpointResolverTest {

    @Test
    void keepsWellFormedEndpoint() {
        assertEquals(
                "https://example.mns.aliyuncs.com/",
                MnsEndpointResolver.resolve("https://example.mns.aliyuncs.com/")
        );
    }

    @Test
    void trimsAndUnquotesEndpoint() {
        assertEquals(
                "https://example.mns.aliyuncs.com/",
                MnsEndpointResolver.resolve("  \"https://example.mns.aliyuncs.com/\"  ")
        );
    }

    @Test
    void addsSchemeWhenMissing() {
        assertEquals(
                "https://example.mns.aliyuncs.com",
                MnsEndpointResolver.resolve("example.mns.aliyuncs.com")
        );
    }

    @Test
    void rejectsBlankEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> MnsEndpointResolver.resolve("   "));
    }
}
