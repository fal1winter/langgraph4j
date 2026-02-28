package io.github.fal1winter.langgraph4j.utils;

import io.github.fal1winter.langgraph4j.core.Edge;
import io.github.fal1winter.langgraph4j.core.Graph;
import io.github.fal1winter.langgraph4j.core.State;

import java.util.*;

/**
 * Utility for visualizing workflow graphs
 * Generates Mermaid diagram syntax
 */
public class GraphVisualizer {

    /**
     * Generate Mermaid flowchart syntax for a graph
     */
    public static <S extends State> String toMermaid(Graph<S> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("graph TD\n");

        // Add entry point
        String entryPoint = graph.getEntryPoint();
        if (entryPoint != null) {
            sb.append("    START([Start]) --> ").append(sanitize(entryPoint)).append("\n");
        }

        // Add nodes
        for (String nodeName : graph.getNodeNames()) {
            sb.append("    ").append(sanitize(nodeName))
              .append("[").append(nodeName).append("]\n");
        }

        // Add END node
        sb.append("    END([End])\n");

        sb.append("```\n");
        return sb.toString();
    }

    /**
     * Generate DOT format for Graphviz
     */
    public static <S extends State> String toDot(Graph<S> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("    rankdir=TB;\n");
        sb.append("    node [shape=box, style=rounded];\n");

        // Add entry point
        String entryPoint = graph.getEntryPoint();
        if (entryPoint != null) {
            sb.append("    START [shape=circle, label=\"Start\"];\n");
            sb.append("    START -> \"").append(entryPoint).append("\";\n");
        }

        // Add nodes
        for (String nodeName : graph.getNodeNames()) {
            sb.append("    \"").append(nodeName).append("\";\n");
        }

        // Add END node
        sb.append("    END [shape=doublecircle, label=\"End\"];\n");

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Generate ASCII art representation
     */
    public static <S extends State> String toAscii(Graph<S> graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("Workflow Graph:\n");
        sb.append("==============\n\n");

        String entryPoint = graph.getEntryPoint();
        if (entryPoint != null) {
            sb.append("Entry: ").append(entryPoint).append("\n");
        }

        sb.append("\nNodes:\n");
        for (String nodeName : graph.getNodeNames()) {
            sb.append("  - ").append(nodeName).append("\n");
        }

        sb.append("\nMax Iterations: ").append(graph.getMaxIterations()).append("\n");

        return sb.toString();
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
