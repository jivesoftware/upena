package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.session.RoutesSessionValidator;
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
    private final Map<String, Session> availableSessions = new ConcurrentHashMap<>();
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
        for (Map.Entry<String, String> entry : session.entrySet()) {
            LOG.info("FIXME: key={} value={}", entry.getKey(), entry.getValue());
        }

        String sessionId = session.get(RoutesSessionValidator.SESSION_ID);
        Session had = sessions.get(sessionId);
        if (had != null) {
            if (had.isValid(expireSessionAfterMillis, expireIdleSessionAfterMillis)) {
                if (had.sessionToken.equals(session.get(RoutesSessionValidator.SESSION_TOKEN))) {
                    had.touch();
                    return true;
                } else {
                    LOG.warn("validation of sessionId={} failed due to session token miss match.", sessionId);
                    return false;
                }
            } else {
                sessions.remove(sessionId);
                return false;
            }
        } else {
            String newSessionToken = nextSessionToken();
            String sessionTokenKey = nextSessionToken();
            availableSessions.put(sessionTokenKey, new Session(System.currentTimeMillis(), newSessionToken));
            // TODO send a link using sessionTokenKey
            // when given sessionTokenKey back and assuming it hasn't expired make session valid
            Session availableSession = availableSessions.remove(sessionTokenKey);
            if (availableSession != null && availableSession.isValid(expireSessionAfterMillis, expireIdleSessionAfterMillis)) {
                availableSession.touch();
                // TODO cookie fun?
                sessions.putIfAbsent(sessionId, availableSession);
                return true;
            } else {
                return false;
            }
        }
    }

    public String nextSessionToken() {
        return new BigInteger(130, random).toString(32);
    }

    private static class Session {

        private final long sessionBirthTimestampMillis;
        private final AtomicLong toucedMillis;
        public final String sessionToken;

        public Session(long sessionBirthTimestampMillis, String sessionToken) {
            this.sessionBirthTimestampMillis = sessionBirthTimestampMillis;
            this.toucedMillis = new AtomicLong(sessionBirthTimestampMillis);
            this.sessionToken = sessionToken;
        }

        public void touch() {
            toucedMillis.set(System.currentTimeMillis());
        }

        public boolean isValid(long expireSessionAfterMillis, long expireIdleSessionAfterMillis) {
            if (sessionBirthTimestampMillis + expireSessionAfterMillis > System.currentTimeMillis()) {
                return false;
            }
            if (toucedMillis.get() + expireIdleSessionAfterMillis > System.currentTimeMillis()) {
                return false;
            }
            return true;
        }
    }
}
