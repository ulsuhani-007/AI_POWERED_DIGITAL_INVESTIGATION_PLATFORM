package com.investigation.service;

import com.investigation.api.ApiClient;
import com.investigation.dto.LoginResponse;
import com.investigation.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {

    private SessionService sessionService;
    private TestApiClient testApiClient;

    static class TestApiClient extends ApiClient {
        Object responseToReturn;
        String lastEndpoint;
        Object lastBody;

        @Override
        public <T> T post(String endpoint, Object requestBody, Class<T> responseType) {
            this.lastEndpoint = endpoint;
            this.lastBody = requestBody;
            if (responseToReturn != null && responseType.isInstance(responseToReturn)) {
                return responseType.cast(responseToReturn);
            }
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        sessionService = SessionService.getInstance();
        sessionService.clearSession();
        sessionService.setUnauthorizedHandler(null);
        testApiClient = new TestApiClient();
    }

    @Test
    void testInitialStateLoggedOut() {
        assertFalse(sessionService.isLoggedIn());
        assertNull(sessionService.getCurrentSession());
        assertNull(sessionService.getToken());
        assertNull(sessionService.getAuthHeader());
    }

    @Test
    void testUserSessionPropertiesAndExpiry() {
        Instant future = Instant.now().plus(2, ChronoUnit.HOURS);
        UserSession activeSession = new UserSession("tok-123", "usr-1", "investigator", "INVESTIGATOR", future);

        assertEquals("tok-123", activeSession.getToken());
        assertEquals("usr-1", activeSession.getUserId());
        assertEquals("investigator", activeSession.getUsername());
        assertEquals("INVESTIGATOR", activeSession.getRole());
        assertEquals(future, activeSession.getExpiresAt());
        assertFalse(activeSession.isExpired());
        assertFalse(activeSession.isAdmin());

        UserSession adminSession = new UserSession("tok-456", "usr-2", "admin", "ADMIN", future);
        assertTrue(adminSession.isAdmin());

        Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);
        UserSession expiredSession = new UserSession("tok-expired", "usr-3", "investigator", "INVESTIGATOR", past);
        assertTrue(expiredSession.isExpired());
    }

    @Test
    void testLoginSuccess() throws Exception {
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        testApiClient.responseToReturn = new LoginResponse("mock-bearer-token", "u-10", "investigator", "INVESTIGATOR", future);

        AtomicBoolean listenerNotified = new AtomicBoolean(false);
        sessionService.addSessionListener(s -> {
            if (s != null && "mock-bearer-token".equals(s.getToken())) {
                listenerNotified.set(true);
            }
        });

        UserSession session = sessionService.login("investigator", "secret", testApiClient);

        assertNotNull(session);
        assertEquals("mock-bearer-token", session.getToken());
        assertTrue(sessionService.isLoggedIn());
        assertEquals("Bearer mock-bearer-token", sessionService.getAuthHeader());
        assertEquals("/api/auth/login", testApiClient.lastEndpoint);
        assertTrue(listenerNotified.get());
    }

    @Test
    void testLogoutClearsSession() {
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        testApiClient.responseToReturn = new LoginResponse("tok-to-logout", "u-10", "investigator", "INVESTIGATOR", future);

        try {
            sessionService.login("investigator", "secret", testApiClient);
        } catch (Exception ignored) {}

        assertTrue(sessionService.isLoggedIn());

        sessionService.logout(testApiClient);

        assertFalse(sessionService.isLoggedIn());
        assertNull(sessionService.getCurrentSession());
        assertNull(sessionService.getToken());
        assertNull(sessionService.getAuthHeader());
        assertEquals("/api/auth/logout", testApiClient.lastEndpoint);
    }

    @Test
    void testClearSessionNotifiesListeners() {
        AtomicBoolean cleared = new AtomicBoolean(false);
        sessionService.addSessionListener(s -> {
            if (s == null) {
                cleared.set(true);
            }
        });

        sessionService.clearSession();
        assertTrue(cleared.get());
    }
}
