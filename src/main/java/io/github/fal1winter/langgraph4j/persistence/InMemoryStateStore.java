package io.github.fal1winter.langgraph4j.persistence;

import io.github.fal1winter.langgraph4j.core.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state store implementation
 * Useful for testing and development
 *
 * @param <S> the state type
 */
public class InMemoryStateStore<S extends State> implements StateStore<S> {

    private final Map<String, S> storage = new ConcurrentHashMap<>();

    @Override
    public void save(String checkpointId, S state) {
        storage.put(checkpointId, state);
    }

    @Override
    public S load(String checkpointId) throws Exception {
        S state = storage.get(checkpointId);
        if (state == null) {
            throw new Exception("Checkpoint not found: " + checkpointId);
        }
        return state;
    }

    @Override
    public boolean exists(String checkpointId) {
        return storage.containsKey(checkpointId);
    }

    @Override
    public void delete(String checkpointId) {
        storage.remove(checkpointId);
    }

    @Override
    public List<String> listCheckpoints() {
        return new ArrayList<>(storage.keySet());
    }

    /**
     * Clear all checkpoints
     */
    public void clear() {
        storage.clear();
    }

    /**
     * Get the number of stored checkpoints
     */
    public int size() {
        return storage.size();
    }
}
