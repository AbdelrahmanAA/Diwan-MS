package com.diwan.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String run(String incoming, MockHttpServletResponse response, AtomicReference<String> seenInChain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (incoming != null) request.addHeader(RequestIdFilter.HEADER, incoming);
        FilterChain chain = (req, res) -> seenInChain.set(MDC.get(RequestIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);
        return response.getHeader(RequestIdFilter.HEADER);
    }

    @Test
    void keepsTheGatewaysIdPutsItInTheLogContextAndEchoesIt() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        String echoed = run("gw-1234abcd", new MockHttpServletResponse(), seen);
        assertEquals("gw-1234abcd", echoed);
        assertEquals("gw-1234abcd", seen.get());
    }

    @Test
    void generatesAnIdWhenMissingOrMalformed() throws Exception {
        for (String bad : new String[]{null, "short", "has spaces in it!", "x".repeat(200), "a\nb-injected-header"}) {
            AtomicReference<String> seen = new AtomicReference<>();
            String echoed = run(bad, new MockHttpServletResponse(), seen);
            assertTrue(echoed.matches("[0-9a-f-]{36}"), "expected a generated UUID for: " + bad);
            assertNotEquals(bad, echoed);
            assertEquals(echoed, seen.get());
        }
    }

    @Test
    void clearsTheLogContextAfterTheRequest() throws Exception {
        run("gw-1234abcd", new MockHttpServletResponse(), new AtomicReference<>());
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }
}
