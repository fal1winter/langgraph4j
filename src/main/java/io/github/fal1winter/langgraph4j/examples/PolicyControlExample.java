package io.github.fal1winter.langgraph4j.examples;

import io.github.fal1winter.langgraph4j.agent.*;
import io.github.fal1winter.langgraph4j.agent.adapters.SimpleLLMAdapter;
import io.github.fal1winter.langgraph4j.core.Graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Example: Using ToolExecutionPolicy for access control
 * Demonstrates how to control which tools can be called and when
 */
public class PolicyControlExample {

    static class SearchTool implements Tool {
        @Override
        public String execute(Object parameters) {
            return "Found papers: " + parameters;
        }

        @Override
        public String getName() {
            return "search";
        }

        @Override
        public String getDescription() {
            return "Search for papers";
        }
    }

    static class DeleteTool implements Tool {
        @Override
        public String execute(Object parameters) {
            return "Deleted: " + parameters;
        }

        @Override
        public String getName() {
            return "delete";
        }

        @Override
        public String getDescription() {
            return "Delete a paper (dangerous!)";
        }
    }

    static class AnalyzeTool implements Tool {
        @Override
        public String execute(Object parameters) {
            return "Analysis result: " + parameters;
        }

        @Override
        public String getName() {
            return "analyze";
        }

        @Override
        public String getDescription() {
            return "Analyze paper content";
        }
    }

    public static void main(String[] args) throws Exception {
        List<Tool> tools = new ArrayList<>();
        tools.add(new SearchTool());
        tools.add(new DeleteTool());
        tools.add(new AnalyzeTool());

        ToolCallingLLM llm = SimpleLLMAdapter.fromFunction(prompt -> {
            if (prompt.contains("delete")) {
                return "I'll delete it.\nTOOL_CALL: delete(paper123)";
            } else if (prompt.contains("search")) {
                return "Let me search.\nTOOL_CALL: search(machine learning)";
            }
            return "FINISH: Done";
        });

        // Example 1: Whitelist - only allow safe tools
        System.out.println("=== Example 1: Whitelist Policy ===");
        ToolExecutionPolicy whitelistPolicy = ToolExecutionPolicy.builder()
            .allowTools("search", "analyze")  // Only these tools allowed
            .maxTotalToolCalls(5)
            .build();

        AutoNode<AgentState> safeNode = AutoNode.<AgentState>builder()
            .llm(llm)
            .tools(tools)
            .policy(whitelistPolicy)
            .build();

        AgentState state1 = new AgentState();
        state1.put("userInput", "delete all papers");
        safeNode.execute(state1);

        System.out.println("Tool calls made:");
        for (AgentState.ToolCall call : state1.getToolCalls()) {
            System.out.println("- " + call.getToolName() + ": " + call.getResult());
        }
        // Output: delete tool will be blocked

        // Example 2: Blacklist - deny dangerous tools
        System.out.println("\n=== Example 2: Blacklist Policy ===");
        ToolExecutionPolicy blacklistPolicy = ToolExecutionPolicy.builder()
            .denyTools("delete")  // Block this tool
            .build();

        AutoNode<AgentState> restrictedNode = AutoNode.<AgentState>builder()
            .llm(llm)
            .tools(tools)
            .policy(blacklistPolicy)
            .build();

        AgentState state2 = new AgentState();
        state2.put("userInput", "search papers");
        restrictedNode.execute(state2);

        // Example 3: Custom policy with hooks
        System.out.println("\n=== Example 3: Custom Policy with Hooks ===");
        ToolExecutionPolicy customPolicy = ToolExecutionPolicy.builder()
            .beforeExecute(context -> {
                System.out.println("  [Policy] Before: " + context.getToolName());
                // Block if too many calls already
                if (context.getTotalToolCallsSoFar() >= 3) {
                    System.out.println("  [Policy] Blocked: too many calls");
                    return false;
                }
                return true;
            })
            .afterExecute(context -> {
                System.out.println("  [Policy] After: " + context.getToolName());
                // Stop if error occurred
                if (context.hasError()) {
                    System.out.println("  [Policy] Stopping due to error");
                    return false;
                }
                return true;
            })
            .maxToolCallsPerIteration(2)  // Max 2 tools per iteration
            .maxTotalToolCalls(10)        // Max 10 tools total
            .build();

        AutoNode<AgentState> monitoredNode = AutoNode.<AgentState>builder()
            .llm(llm)
            .tools(tools)
            .policy(customPolicy)
            .build();

        AgentState state3 = new AgentState();
        state3.put("userInput", "search papers");
        monitoredNode.execute(state3);

        // Example 4: Use in workflow
        System.out.println("\n=== Example 4: Policy in Workflow ===");
        Graph<AgentState> workflow = Graph.<AgentState>builder()
            .addNode("research", AutoNode.<AgentState>builder()
                .llm(llm)
                .tools(tools)
                .policy(ToolExecutionPolicy.builder()
                    .allowTools("search", "analyze")  // Research phase: safe tools only
                    .maxTotalToolCalls(3)
                    .build())
                .build())

            .addNode("validate", state -> {
                System.out.println("Validating results...");
                if (state.getToolCalls().isEmpty()) {
                    state.setError("No research done");
                }
                return state;
            })

            .setEntryPoint("research")
            .addEdge("research", "validate")
            .addEdge("validate", Graph.END);

        AgentState state4 = new AgentState();
        state4.put("userInput", "search and analyze papers");
        AgentState result = workflow.execute(state4);

        System.out.println("\nFinal tool calls:");
        for (AgentState.ToolCall call : result.getToolCalls()) {
            System.out.println("- " + call);
        }
    }
}
