package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.server.session.RouteSessionValidator;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author jonathan.colt
 */
public class SessionStore {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Session> tokenAccessibleSessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final long expireSessionAfterMillis;
    private final long expireIdleSessionAfterMillis;

    public SessionStore(long expireSessionAfterMillis, long expireIdleSessionAfterMillis) {
        this.expireSessionAfterMillis = expireSessionAfterMillis;
        this.expireIdleSessionAfterMillis = expireIdleSessionAfterMillis;
    }

    public boolean isValid(SessionValidation session) {
        if (session == null) {
            return false;
        }

        String sessionId = session.get(RouteSessionValidator.SESSION_ID);
        String sessionToken = session.get(RouteSessionValidator.SESSION_TOKEN);
        Session had = sessions.get(sessionToken);
        if (had != null) {
            if (had.isValid(expireSessionAfterMillis, expireIdleSessionAfterMillis) ) {
                if (had.sessionId.equals(sessionId)) {
                    had.touch();
                    return true;
                } else {
                    LOG.warn("Validation of sessionId={} failed due to session mismatch.", sessionId);
                    return false;
                }
            } else {
                sessions.remove(sessionId);
                return false;
            }
        }
        return false;
    }

    public String exchangeAccessForSession(String sessionId, String accessToken) {
        Session availableSession = tokenAccessibleSessions.remove(accessToken);
        if (availableSession != null
            && availableSession.isValid(expireSessionAfterMillis, expireIdleSessionAfterMillis)
            && availableSession.sessionId.equals(sessionId)) {
            availableSession.touch();
            sessions.putIfAbsent(availableSession.sessionToken, availableSession);
            return availableSession.sessionToken;
        } else {
            return null;
        }
    }

    public String generateAccessToken(String instanceKey) {
        String accessToken = nextSessionToken();
        String newSessionToken = nextSessionToken();
        tokenAccessibleSessions.put(accessToken, new Session(System.currentTimeMillis(), instanceKey, newSessionToken));
        return accessToken;
    }

    public String nextSessionToken() {
        return new BigInteger(130, random).toString(32);
    }

    private static class Session {

        private final long sessionBirthTimestampMillis;
        private final AtomicLong touchedMillis;
        public final String sessionId;
        public final String sessionToken;

        public Session(long sessionBirthTimestampMillis, String sessionId, String sessionToken) {
            this.sessionBirthTimestampMillis = sessionBirthTimestampMillis;
            this.touchedMillis = new AtomicLong(sessionBirthTimestampMillis);
            this.sessionId = sessionId;
            this.sessionToken = sessionToken;
        }

        public void touch() {
            touchedMillis.set(System.currentTimeMillis());
        }

        public boolean isValid(long expireSessionAfterMillis, long expireIdleSessionAfterMillis) {
            long now = System.currentTimeMillis();
            if (sessionBirthTimestampMillis + expireSessionAfterMillis < now) {
                return false;
            }
            if (touchedMillis.get() + expireIdleSessionAfterMillis < now) {
                return false;
            }
            return true;
        }
    }
}
