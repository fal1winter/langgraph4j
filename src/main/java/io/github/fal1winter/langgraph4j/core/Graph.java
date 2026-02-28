package io.github.fal1winter.langgraph4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

/**
 * The main workflow graph class
 * Provides a fluent API for building and executing stateful workflows
 *
 * @param <S> the state type
 */
public class Graph<S extends State> {

    private static final Logger log = LoggerFactory.getLogger(Graph.class);

    public static final String END = "__END__";

    private final Map<String, Node<S>> nodes = new LinkedHashMap<>();
    private final Map<String, List<Edge<S>>> edges = new HashMap<>();
    private final Map<String, Router<S>> routers = new HashMap<>();
    private final List<GraphListener<S>> listeners = new ArrayList<>();

    private String entryPoint;
    private int maxIterations = 100;
    private boolean enableLogging = true;

    /**
     * Add a node to the graph
     */
    public Graph<S> addNode(String name, Node<S> node) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Node name cannot be null or empty");
        }
        if (name.equals(END)) {
            throw new IllegalArgumentException("Node name cannot be '__END__'");
        }
        nodes.put(name, node);
        if (enableLogging) {
            log.debug("Added node: {}", name);
        }
        return this;
    }

    /**
     * Set the entry point of the graph
     */
    public Graph<S> setEntryPoint(String nodeName) {
        if (!nodes.containsKey(nodeName)) {
            throw new IllegalArgumentException("Node does not exist: " + nodeName);
        }
        this.entryPoint = nodeName;
        if (enableLogging) {
            log.debug("Set entry point: {}", nodeName);
        }
        return this;
    }

    /**
     * Add an unconditional edge
     */
    public Graph<S> addEdge(String from, String to) {
        validateNodeExists(from);
        if (!to.equals(END)) {
            validateNodeExists(to);
        }
        edges.computeIfAbsent(from, k -> new ArrayList<>())
              .add(new Edge<>(from, to));
        if (enableLogging) {
            log.debug("Added edge: {} -> {}", from, to);
        }
        return this;
    }

    /**
     * Add a conditional edge
     */
    public Graph<S> addConditionalEdge(String from, String to, Predicate<S> condition) {
        return addConditionalEdge(from, to, condition, null);
    }

    /**
     * Add a labeled conditional edge
     */
    public Graph<S> addConditionalEdge(String from, String to, Predicate<S> condition, String label) {
        validateNodeExists(from);
        if (!to.equals(END)) {
            validateNodeExists(to);
        }
        edges.computeIfAbsent(from, k -> new ArrayList<>())
              .add(new Edge<>(from, to, condition, label));
        if (enableLogging) {
            log.debug("Added conditional edge: {} -> {} [{}]", from, to, label != null ? label : "unlabeled");
        }
        return this;
    }

    /**
     * Add a router for multi-way branching
     */
    public Graph<S> addRouter(String from, Router<S> router) {
        validateNodeExists(from);
        routers.put(from, router);
        if (enableLogging) {
            log.debug("Added router at node: {}", from);
        }
        return this;
    }

    /**
     * Add a listener for graph events
     */
    public Graph<S> addListener(GraphListener<S> listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * Set maximum iterations to prevent infinite loops
     */
    public Graph<S> setMaxIterations(int maxIterations) {
        if (maxIterations <= 0) {
            throw newmentException("Max iterations must be positive");
        }
        this.maxIterations = maxIterations;
        return this;
    }

    /**
     * Enable or disable logging
     */
    public Graph<S> setLogging(boolean enabled) {
        this.enableLogging = enabled;
        return this;
    }

    /**
     * Execute the workflow
     */
    public S execute(S initialState) throws Exception {
        if (entryPoint == null) {
            throw new IllegalStateException("Entry point not set");
        }

        S state = initialState;
        String currentNode = entryPoint;
        Set<String> visitedNodes = new HashSet<>();
        List<String> executionPath = new ArrayList<>();
        int iterations = 0;

        notifyStart(state);

        if (enableLogging) {
            log.info("Starting workflow execution from: {}", entryPoint);
        }

        while (!END.equals(currentNode) && iterations < maxIterations) {
            iterations++;
            executionPath.add(currentNode);

            if (enableLogging) {
                log.info("Executing node [{}] (iteration {})", currentNode, iterations);
            }

            // Execute node
            Node<S> node = nodes.get(currentNode);
            if (node == null) {
                throw new IllegalStateException("Node not found: " + currentNode);
            }

            notifyBeforeNode(currentNode, state);

            try {
                state = node.execute(state);
                notifyAfterNode(currentNode, state);

                if (enableLogging) {
                    log.info("Node [{}] completed successfully", currentNode);
                }
            } catch (Exception e) {
                if (enableLogging) {
                    log.error("Node [{}] failed: {}", currentNode, e.getMessage());
                }
                state.setError("Node " + currentNode + " failed: " + e.getMessage());
                notifyError(currentNode, state, e);
                throw e;
            }

            // Check for human input requirement
            if (state.isNeedsHumanInput()) {
                if (enableLogging) {
                    log.info("Node [{}] requires human input, pausing execution", currentNode);
                }
                notifyHumanInputRequired(currentNode, state);
                return state;
            }

            // Determine next node
            visitedNodes.add(currentNode);
            String nextNode = determineNextNode(currentNode, state);

            if (enableLogging) {
                log.info("Transitioning: {} -> {}", currentNode, nextNode);
            }

            notifyTransition(currentNode, nextNode, state);
            currentNode = nextNode;
        }

        if (iterations >= maxIterations) {
            String error = "Workflow exceeded maximum iterations: " + maxIterations;
            if (enableLogging) {
                log.error(error);
            }
            state.setError(error);
        } else {
            if (enableLogging) {
                log.info("Workflow completed successfully after {} iterations", iterations);
                log.info("Execution path: {}", String.join(" -> ", executionPath));
            }
        }

        notifyComplete(state);
        return state;
    }

    /**
     * Determine the next node based on edges and routers
     */
    private String determineNextNode(String currentNode, S state) {
        // Check router first
        if (routers.containsKey(currentNode)) {
            Router<S> router = routers.get(currentNode);
            String nextNode = router.route(state);
            if (enableLogging) {
                log.debug("Router selected: {}", nextNode);
            }
            return nextNode;
        }

        // Check edges
        List<Edge<S>> nodeEdges = edges.get(currentNode);
        if (nodeEdges == null || nodeEdges.isEmpty()) {
            if (enableLogging) {
                log.debug("No outgoing edges from {}, ending workflow", currentNode);
            }
            return END;
        }

        // Find first matching edge
        for (Edge<S> edge : nodeEdges) {
            if (edge.shouldTransition(state)) {
                if (enableLogging && edge.isConditional()) {
                    log.debug("Conditional edge matched: {}", edge);
                }
                return edge.getToNode();
            }
        }

        // No matching edge
        if (enableLogging) {
            log.debug("No matching edges from {}, ending workflow", currentNode);
        }
        return END;
    }

    /**
     * Validate that a node exists
     */
    private void validateNodeExists(String nodeName) {
        if (!nodes.containsKey(nodeName)) {
            throw new IllegalArgumentException("Node does not exist: " + nodeName);
        }
    }

    // Listener notifications

    private void notifyStart(S state) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onStart(state);
            } catch (Exception e) {
                log.warn("Listener error in onStart", e);
            }
        }
    }

    private void notifyBeforeNode(String nodeName, S state) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onBeforeNode(nodeName, state);
            } catch (Exception e) {
                log.warn("Listener error in onBeforeNode", e);
            }
        }
    }

    private void notifyAfterNode(String nodeName, S state) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onAfterNode(nodeName, state);
            } catch (Exception e) {
                log.warn("Listener error in onAfterNode", e);
            }
        }
    }

    private void notifyTransition(String from, String to, S state) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onTransition(from, to, state);
            } catch (Exception e) {
                log.warn("Listener error in onTransition", e);
            }
        }
    }

    private void notifyHumanInputRequired(String nodeName, S state) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onHumanInputRequired(nodeName, state);
            } catch (Exception e) {
                log.warn("Listener error in onHumanInputRequired", e);
            }
        }
    }

    private void notifyError(String nodeName, S state, Exception error) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onError(nodeName, state, error);
            } catch (Exception e) {
                log.warn("Listener error in onError", e);
            }
        }
    }

    private void notifyComplete(S state) {
        for (GraphListener<S> listener : listeners) {
            try {
                listener.onComplete(state);
            } catch (Exception e) {
                log.warn("Listener error in onComplete", e);
            }
        }
    }

    // Builder pattern

    public static <S extends State> Graph<S> builder() {
        return new Graph<>();
    }

    // Getters for introspection

    public Set<String> getNodeNames() {
        return new HashSet<>(nodes.keySet());
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public int getMaxIterations() {
        return maxIterations;
    }
}
