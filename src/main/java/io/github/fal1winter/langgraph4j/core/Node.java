package io.github.fal1winter.langgraph4j.core;

/**
 * Functional interface representing a workflow node
 * A node is a unit of work that transforms the state
 *
 * @param <S> the state type
 */
@FunctionalInterface
public interface Node<S extends State> {

    /**
     * Execute the node logic
     *
     * @param state the current state
     * @return the updated state
     * @throws Exception if execution fails
     */
    S execute(S state) throws Exception;
}
