package io.github.fal1winter.langgraph4j.core;

/**
 * Functional interface for conditional routing
 * Routes to different nodes based on state
 *
 * @param <S> the state type
 */
@FunctionalInterface
public interface Router<S extends State> {

    /**
     * Determine the next node based on the current state
     *
     * @param state the current state
     * @return the name of the next node, or Graph.END to terminate
     */
    String route(S state);
}
