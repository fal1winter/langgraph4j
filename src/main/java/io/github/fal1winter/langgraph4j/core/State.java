package io.github.fal1winter.langgraph4j.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Base state class for workflow execution
 * Provides a flexible key-value store for workflow data
 */
public class State {

    private final Map<String, Object> data = new HashMap<>();

    private String error;
    private boolean needsHumanInput = false;
    private String humanInput;

    /**
     * Put a value into the state
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Get a value from the state
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * Get a value with default
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = data.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Check if key exists
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    /**
     * Remove a key
     */
    public void remove(String key) {
        data.remove(key);
    }

    /**
     * Get all data
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    /**
     * Clear all data
     */
    public void clear() {
        data.clear();
    }

    // Error handling

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    // Human-in-the-loop support

    public boolean isNeedsHumanInput() {
        return needsHumanInput;
    }

    public void setNeedsHumanInput(boolean needsHumanInput) {
        this.needsHumanInput = needsHumanInput;
    }

    public String getHumanInput() {
        return humanInput;
    }

    public void setHumanInput(String humanInput) {
        this.humanInput = humanInput;
        this.needsHumanInput = false;
    }

    @Override
    public String toString() {
        return "State{" +
                "data=" + data +
                ", error='" + error + '\'' +
                ", needsHumanInput=" + needsHumanInput +
                '}';
    }
}
