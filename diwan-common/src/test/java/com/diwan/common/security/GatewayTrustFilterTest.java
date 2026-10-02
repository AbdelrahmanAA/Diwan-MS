package com.diwan.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayTrustFilterTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";
    private final GatewaySigner signer = GatewaySigner.fromSecret(SECRET);

    private HttpServletRequest request(String path, String userId, String email, String role, Long ts, String sig) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(path);
        when(r.getHeader(GatewaySigner.HDR_USER_ID)).thenReturn(userId);
        when(r.getHeader(GatewaySigner.HDR_EMAIL)).thenReturn(email);
        when(r.getHeader(GatewaySigner.HDR_ROLE)).thenReturn(role);
        when(r.getHeader(GatewaySigner.HDR_TIMESTAMP)).thenReturn(ts == null ? null : String.valueOf(ts));
        when(r.getHeader(GatewaySigner.HDR_SIGNATURE)).thenReturn(sig);
        return r;
    }

    @Test
    void signerRejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> GatewaySigner.fromSecret("short"));
    }

    @Test
    void signatureBindsEveryField() {
        long ts = 1000L;
        String sig = signer.sign("7", "a@b.c", "USER", ts);
        assertTrue(signer.verify("7", "a@b.c", "USER", ts, sig));
        assertFalse(signer.verify("8", "a@b.c", "USER", ts, sig));
        assertFalse(signer.verify("7", "x@b.c", "USER", ts, sig));
        assertFalse(signer.verify("7", "a@b.c", "ADMIN", ts, sig));
        assertFalse(signer.verify("7", "a@b.c", "USER", ts + 1, sig));
        assertFalse(GatewaySigner.fromSecret(SECRET + "x").verify("7", "a@b.c", "USER", ts, sig));
    }

    @Test
    void allowsValidSignedRequest() throws Exception {
        long ts = System.currentTimeMillis();
        String sig = signer.sign("7", "a@b.c", "USER", ts);
        HttpServletRequest req = request("/api/x", "7", "a@b.c", "USER", ts, sig);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new GatewayTrustFilter(signer, List.of()).doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    void rejectsForgedLegacyHeaders() throws Exception {
        HttpServletRequest req = request("/api/x", "7", null, null, null, null);
        when(req.getHeader("X-Gateway-Validated")).thenReturn("true");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new GatewayTrustFilter(signer, List.of()).doFilter(req, res, chain);
        verify(res).sendError(anyInt(), anyString());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsTamperedUserIdAndStaleTimestamp() throws Exception {
        long ts = System.currentTimeMillis();
        String sig = signer.sign("7", "a@b.c", "USER", ts);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new GatewayTrustFilter(signer, List.of()).doFilter(request("/api/x", "8", "a@b.c", "USER", ts, sig), res, chain);
        verify(res).sendError(401, "Unauthorized: invalid gateway signature");

        long old = ts - 10 * 60_000;
        String oldSig = signer.sign("7", "a@b.c", "USER", old);
        HttpServletResponse res2 = mock(HttpServletResponse.class);
        new GatewayTrustFilter(signer, List.of()).doFilter(request("/api/x", "7", "a@b.c", "USER", old, oldSig), res2, chain);
        verify(res2).sendError(401, "Unauthorized: invalid gateway signature");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void enforcesRequiredRole() throws Exception {
        long ts = System.currentTimeMillis();
        String sig = signer.sign("7", "a@b.c", "USER", ts);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new GatewayTrustFilter(signer, List.of(), "ADMIN").doFilter(request("/api/x", "7", "a@b.c", "USER", ts, sig), res, chain);
        verify(res).sendError(403, "Forbidden");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void publicPathPassesButHidesIdentityHeaders() throws Exception {
        HttpServletRequest req = request("/api/users/login", "7", null, null, null, null);
        when(req.getHeaderNames()).thenReturn(Collections.enumeration(List.of("X-User-Id", "Content-Type")));
        HttpServletResponse res = mock(HttpServletResponse.class);
        AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
        FilterChain chain = (r, s) -> seen.set((HttpServletRequest) r);
        new GatewayTrustFilter(signer, List.of("/api/users/login")).doFilter(req, res, chain);
        assertNull(seen.get().getHeader("x-user-id"));
        assertEquals(List.of("Content-Type"), Collections.list(seen.get().getHeaderNames()));
    }

    @Test
    void publicPrefixMatchesOnSegmentBoundaryOnly() throws Exception {
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new GatewayTrustFilter(signer, List.of("/api/smart-home/alexa"))
                .doFilter(request("/api/smart-home/alexa-evil", null, null, null, null, null), res, chain);
        verify(res).sendError(401, "Unauthorized: request did not pass Gateway validation");
    }
}
