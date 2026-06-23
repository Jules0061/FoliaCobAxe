package dev.Jules.foliaCobAxe.cooldown;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final ConcurrentHashMap<UUID, Long> expiry = new ConcurrentHashMap<>();

    public long remainingMillis(UUID playerId) {
        Long until = expiry.get(playerId);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            expiry.remove(playerId, until);
            return 0L;
        }
        return remaining;
    }

    public void apply(UUID playerId, long seconds) {
        if (seconds <= 0L) {
            return;
        }
        expiry.put(playerId, System.currentTimeMillis() + seconds * 1000L);
    }

    public void clear(UUID playerId) {
        expiry.remove(playerId);
    }

    public void clearAll() {
        expiry.clear();
    }
}
