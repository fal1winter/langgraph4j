package io.github.fal1winter.langgraph4j.examples;

import io.github.fal1winter.langgraph4j.agent.*;
import io.github.fal1winter.langgraph4j.agent.adapters.SimpleLLMAdapter;
import io.github.fal1winter.langgraph4j.core.Graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Example: Using AutoNode for automatic tool calling
 * Demonstrates hybrid approach: LangGraph orchestration + LangChain-style auto tool calling
 */
public class AutoNodeExample {

    // Define some simple tools
    static class SearchTool implements Tool {
        @Override
        public String execute(Object parameters) {
            return "Found 3 papers about: " + parameters;
        }

        @Override
        public String getName() {
            return "search";
        }

        @Override
        public String getDescription() {
            return "Search for papers by keyword";
        }
    }

    static class GetDetailTool implements Tool {
        @Override
        public String execute(Object parameters) {
            return "Paper details for ID " + parameters + ": Title, Authors, Abstract...";
        }

        @Override
        public String getName() {
            return "getDetail";
        }

        @Override
        public String getDescription() {
            return "Get paper details by ID";
        }
    }

    static class CalculateTool implements Tool {
        @Override
        public String execute(Object parameters) {
            return "Calculation result: 42";
        }

        @Override
        public String getName() {
            return "calculate";
        }

        @Override
        public String getDescription() {
            return "Perform calculations";
        }
    }

    public static void main(String[] args) throws Exception {
        // Create tools
        List<Tool> tools = new ArrayList<>();
        tools.add(new SearchTool());
        tools.add(new GetDetailTool());
        tools.add(new CalculateTool());

        // Create a simple LLM adapter (in real use, connect to actual LLM)
        ToolCallingLLM llm = SimpleLLMAdapter.fromFunction(prompt -> {
            System.out.println("\n[LLM Prompt]:\n" + prompt);

            // Simulate LLM decision making
            if (prompt.contains("search for machine learning")) {
                return "I need to search first.\nTOOL_CALL: search(machine learning)";
            } else if (prompt.contains("Found 3 papers")) {
                return "Now let me get details of the first paper.\nTOOL_CALL: getDetail(1)";
            } else if (prompt.contains("Paper details")) {
                return "FINISH: Based on the search and details, I found relevant papers about machine learning.";
            } else {
                return "FINISH: I don't need any tools for this request.";
            }
        });

        // Build workflow with AutoNode
        Graph<AgentState> workflow = Graph.<AgentState>builder()
            // Node 1: Auto node that can call tools automatically
            .addNode("agent", AutoNode.<AgentState>builder()
                .llm(llm)
                .tools(tools)
                .maxIterations(5)
                .systemPrompt("You are a helpful assistant. Use tools when needed.")
                .build())

            // Node 2: Format final response
            .addNode("format_response", state -> {
                String response = state.getLLMResponse();
                state.put("finalResponse", "Final Answer: " + response);
                System.out.println("\n[Final Response]: " + response);
                return state;
            })

            .setEntryPoint("agent")
            .addEdge("agent", "format_response")
            .addEdge("format_response", Graph.END);

        // Execute workflow
        System.out.println("=== Example 1: Search and Get Details ===");
        AgentState state1 = new AgentState();
        state1.put("userInput", "search for machine learning papers");
        AgentState result1 = workflow.execute(state1);

        System.out.println("\n=== Tool Calls Made ===");
        for (AgentState.ToolCall call : result1.getToolCalls()) {
            System.out.println("- " + call.getToolName() + ": " + call.getResult());
        }

        // Example 2: Simple query without tools
        System.out.println("\n\n=== Example 2: Simple Query ===");
        AgentState state2 = new AgentState();
        state2.put("userInput", "what is 2+2?");
        AgentState result2 = workflow.execute(state2);
    }
}
