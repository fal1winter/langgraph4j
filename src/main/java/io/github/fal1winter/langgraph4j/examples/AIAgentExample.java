package io.github.fal1winter.langgraph4j.examples;

import io.github.fal1winter.langgraph4j.core.Graph;
import io.github.fal1winter.langgraph4j.core.GraphListener;
import io.github.fal1winter.langgraph4j.core.State;
import io.github.fal1winter.langgraph4j.persistence.InMemoryStateStore;

/**
 * Advanced example: AI Agent workflow with checkpointing
 * Demonstrates state persistence and resumption
 */
public class AIAgentExample {

    static class AgentState extends State {
        public AgentState(String query) {
            put("query", query);
        }

        public String getQuery() {
            return get("query");
        }

        public void setIntent(String intent) {
            put("intent", intent);
        }

        public String getIntent() {
            return get("intent");
        }

        public void setToolResult(String result) {
            put("toolResult", result);
        }

        public String getToolResult() {
            return get("toolResult");
        }

        public void setFinalResponse(String response) {
            put("finalResponse", response);
        }

        public String getFinalResponse() {
            return get("finalResponse");
        }
    }

    public static void main(String[] args) throws Exception {
        // Create state store for checkpointing
        InMemoryStateStore<AgentState> stateStore = new InMemoryStateStore<>();

        // Build AI agent workflow
        Graph<AgentState> agent = Graph.<AgentState>builder()
            // Node 1: Analyze user intent
            .addNode("analyze_intent", state -> {
                System.out.println("Analyzing intent for: " + state.getQuery());
                String query = state.getQuery().toLowerCase();

                String intent;
                if (query.contains("search") || query.contains("find")) {
                    intent = "SEARCH";
                } else if (query.contains("calculate") || query.contains("compute")) {
                    intent = "CALCULATE";
                } else {
                    intent = "CHAT";
                }

                state.setIntent(intent);
                System.out.println("Detected intent: " + intent);
                return state;
            })
            // Node 2: Search tool
            .addNode("search_tool", state -> {
                System.out.println("Executing search tool...");
                String result = "Found 3 results for: " + state.getQuery();
                state.setToolResult(result);
                return state;
            })
            // Node 3: Calculate tool
            .addNode("calculate_tool", state -> {
                System.out.println("Executing calculate tool...");
                String result = "Calculation result: 42";
                state.setToolResult(result);
                return state;
            })
            // Node 4: Simple chat
            .addNode("chat", state -> {
                System.out.println("Generating chat response...");
                String result = "I understand you said: " + state.getQuery();
                state.setToolResult(result);
                return state;
            })
            // Node 5: Generate final response
            .addNode("generate_response", state -> {
                System.out.println("Generating final response...");
                String response = "Based on " + state.getIntent() + ": " + state.getToolResult();
                state.setFinalResponse(response);
                return state;
            })
            .setEntryPoint("analyze_intent")
            // Dynamic routing based on intent
            .addRouter("analyze_intent", state -> {
                switch (state.getIntent()) {
                    case "SEARCH": return "search_tool";
                    case "CALCULATE": return "calculate_tool";
                    default: return "chat";
                }
            })
            .addEdge("search_tool", "generate_response")
            .addEdge("calculate_tool", "generate_response")
            .addEdge("chat", "generate_response")
            .addEdge("generate_response", Graph.END)
            // Add listener for checkpointing
            .addListener(new GraphListener<AgentState>() {
                @Override
                public void onAfterNode(String nodeName, AgentState state) {
                    try {
                        String checkpointId = "checkpoint_" + nodeName;
                        stateStore.save(checkpointId, state);
                        System.out.println("  [Checkpoint saved: " + checkpointId + "]");
                    } catch (Exception e) {
                        System.err.println("Failed to save checkpoint: " + e.getMessage());
                    }
                }
            });

        // Test case 1: Search query
        System.out.println("=== Test 1: Search Query ===");
        AgentState state1 = new AgentState("search for machine learning papers");
        AgentState result1 = agent.execute(state1);
        System.out.println("Final response: " + result1.getFinalResponse());

        // Test case 2: Calculate query
        System.out.println("\n=== Test 2: Calculate Query ===");
        AgentState state2 = new AgentState("calculate the sum of 20 and 22");
        AgentState result2 = agent.execute(state2);
        System.out.println("Final response: " + result2.getFinalResponse());

        // Test case 3: Chat query
        System.out.println("\n=== Test 3: Chat Query ===");
        AgentState state3 = new AgentState("hello, how are you?");
        AgentState result3 = agent.execute(state3);
        System.out.println("Final response: " + result3.getFinalResponse());

        // Demonstrate checkpoint loading
        System.out.println("\n=== Checkpoints ===");
        System.out.println("Available checkpoints: " + stateStore.listCheckpoints());
    }
}
