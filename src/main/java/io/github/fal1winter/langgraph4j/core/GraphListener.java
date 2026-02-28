package io.github.fal1winter.langgraph4j.core;

/**
 * Listener interface for graph execution events
 * Allows monitoring and reacting to workflow execution
 *
 * @param <S> the state type
 */
public interface GraphListener<S extends State> {

    /**
     * Called when workflow execution starts
     */
    default void onStart(S state) {}

    /**
     * Called before a node executes
     */
    default void onBeforeNode(String nodeName, S state) {}

    /**
     * Called after a node executes successfully
     */
    default void onAfterNode(String nodeName, S state) {}

    /**
     * Called when transitioning between nodes
     */
    default void onTransition(String from, String to, S state) {}

    /**
     * Called when a node requires human input
     */
    default void onHumanInputRequired(String nodeName, S state) {}

    /**
     * Called when a node execution fails
     */
    default void onError(String nodeName, S state, Exception error) {}

    /**
     * Called when workflow execution completes
     */
    default void onComplete(S state) {}
}
