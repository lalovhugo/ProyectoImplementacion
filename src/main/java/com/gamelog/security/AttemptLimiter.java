package com.gamelog.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bloquea 5 minutos después de 5 códigos incorrectos seguidos (en memoria). */
@Component
public class AttemptLimiter {

    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 5 * 60 * 1000;

    private record State(int fails, long lockedUntil) {}

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        State s = states.get(key);
        return s != null && s.lockedUntil() > System.currentTimeMillis();
    }

    public void fail(String key) {
        states.merge(key, new State(1, 0), (old, ignored) -> {
            int fails = old.fails() + 1;
            return fails >= MAX_FAILS
                    ? new State(0, System.currentTimeMillis() + LOCK_MS)
                    : new State(fails, 0);
        });
    }

    public void reset(String key) {
        states.remove(key);
    }
}