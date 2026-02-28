package io.github.fal1winter.langgraph4j.core;

import java.util.function.Predicate;

/**
 * Represents an edge between two nodes in the workflow graph
 * Edges can be conditional or unconditional
 *
 * @param <S> the state type
 */
public class Edge<S extends State> {

    private final String fromNode;
    private final String toNode;
    private final Predicate<S> condition;
    private final String label;

    /**
     * Create an unconditional edge
     */
    public Edge(String fromNode, String toNode) {
        this(fromNode, toNode, null, null);
    }

    /**
     * Create a conditional edge
     */
    public Edge(String fromNode, String toNode, Predicate<S> condition) {
        this(fromNode, toNode, condition, null);
    }

    /**
     * Create a labeled conditional edge
     */
    public Edge(String fromNode, String toNode, Predicate<S> condition, String label) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.condition = condition;
        this.label = label;
    }

    public String getFromNode() {
        return fromNode;
    }

    public String getToNode() {
        return toNode;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Check if this edge should be taken based on the current state
     */
    public boolean shouldTransition(S state) {
        return condition == null || condition.test(state);
    }

    public boolean isConditional() {
        return condition != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fromNode).append(" -> ").append(toNode);
        if (label != null) {
            sb.append(" [").append(label).append("]");
        }
        if (condition != null) {
            sb.append(" (conditional)");
        }
        return sb.toString();
    }
}
