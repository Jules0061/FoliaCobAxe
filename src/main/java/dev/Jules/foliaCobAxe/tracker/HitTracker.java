package dev.Jules.foliaCobAxe.tracker;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HitTracker {

    private final ConcurrentHashMap<UUID, Integer> hits = new ConcurrentHashMap<>();

    public int increment(UUID playerId) {
        return hits.merge(playerId, 1, Integer::sum);
    }

    public int get(UUID playerId) {
        return hits.getOrDefault(playerId, 0);
    }

    public void reset(UUID playerId) {
        hits.remove(playerId);
    }

    public void clear() {
        hits.clear();
    }
}
