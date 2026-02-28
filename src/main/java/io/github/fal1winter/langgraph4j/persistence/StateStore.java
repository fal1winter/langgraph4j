package io.github.fal1winter.langgraph4j.persistence;

import io.github.fal1winter.langgraph4j.core.State;

/**
 * Interface for persisting workflow state
 * Enables checkpointing and resuming workflows
 *
 * @param <S> the state type
 */
public interface StateStore<S extends State> {

    /**
     * Save state with a checkpoint ID
     */
    void save(String checkpointId, S state) throws Exception;

    /**
     * Load state from a checkpoint
     */
    S load(String checkpointId) throws Exception;

    /**
     * Check if a checkpoint exists
     */
    boolean exists(String checkpointId);

    /**
     * Delete a checkpoint
     */
    void delete(String checkpointId) throws Exception;

    /**
     * List all checkpoint IDs
     */
    java.util.List<String> listCheckpoints();
}
