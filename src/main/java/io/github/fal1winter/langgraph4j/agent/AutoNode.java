package io.github.fal1winter.langgraph4j.agent;

import io.github.fal1winter.langgraph4j.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto node that can automatically call tools based on LLM decisions
 * Combines the flexibility of LangGraph with the intelligence of LangChain
 */
public class AutoNode<S extends AgentState> implements Node<S> {

    private static final Logger log = LoggerFactory.getLogger(AutoNode.class);

    private final ToolCallingLLM llm;
    private final List<Tool> tools;
    private final int maxIterations;
    private final String systemPrompt;

    private AutoNode(Builder<S> builder) {
        this.llm = builder.llm;
        this.tools = builder.tools;
        this.maxIterations = builder.maxIterations;
        this.systemPrompt = builder.systemPrompt;
    }

    @Override
    public S execute(S state) throws Exception {
        log.info("AutoNode executing with {} tools available", tools.size());

        // Build prompt with context
        String prompt = buildPrompt(state);

        int iteration = 0;
        boolean shouldContinue = true;

        while (shouldContinue && iteration < maxIterations) {
            iteration++;
            log.debug("AutoNode iteration {}/{}", iteration, maxIterations);

            // Call LLM with available tools
            ToolCallingLLM.LLMResponse response = llm.generate(prompt, tools);

            // Store LLM response
            state.setLLMResponse(response.getText());

            // Check if LLM wants to call tools
            if (response.hasToolCalls()) {
                log.info("LLM requested {} tool calls", response.getToolCalls().size());

                // Execute each tool call
                StringBuilder toolResults = new StringBuilder();
                for (ToolCallingLLM.ToolCallRequest toolCall : response.getToolCalls()) {
                    String result = executeToolCall(state, toolCall);
                    toolResults.append("\n[").append(toolCall.getToolName())
                               .append(" result]: ").append(result);
                }

                // Update prompt with tool results for next iteration
                prompt = prompt + "\n\nTool execution results:" + toolResults.toString() +
                         "\n\nBased on these results, what should we do next?";

                shouldContinue = !response.isFinished();
            } else {
                // No more tool calls, we're done
                log.info("AutoNode completed, no more tool calls");
                shouldContinue = false;
            }
        }

        if (iteration >= maxIterations) {
            log.warn("AutoNode reached max iterations ({})", maxIterations);
            state.setError("AutoNode exceeded maximum iterations");
        }

        state.setShouldContinue(false);
        return state;
    }

    /**
     * Execute a single tool call
     */
    private String executeToolCall(S state, ToolCallingLLM.ToolCallRequest toolCall) {
        String toolName = toolCall.getToolName();
        log.info("Executing tool: {}", toolName);

        try {
            // Find the tool
            Tool tool = findTool(toolName);
            if (tool == null) {
                String error = "Tool not found: " + toolName;
                log.error(error);
                state.addToolCall(toolName, toolCall.getParameters(), error);
                return error;
            }

            // Execute the tool
            String result = tool.execute(toolCall.getParameters());
            log.info("Tool {} executed successfully", toolName);

            // Record the tool call
            state.addToolCall(toolName, toolCall.getParameters(), result);

            return result;
        } catch (Exception e) {
            String error = "Tool execution failed: " + e.getMessage();
            log.error("Tool {} failed: {}", toolName, e.getMessage());
            state.addToolCall(toolName, toolCall.getParameters(), error);
            return error;
        }
    }

    /**
     * Find tool by name
     */
    private Tool findTool(String name) {
        for (Tool tool : tools) {
            if (tool.getName().equals(name)) {
                return tool;
            }
        }
        return null;
    }

    /**
     * Build prompt with system message and context
     */
    private String buildPrompt(S state) {
        StringBuilder prompt = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            prompt.append(systemPrompt).append("\n\n");
        }

        // Add available tools description
        prompt.append("Available tools:\n");
        for (Tool tool : tools) {
            prompt.append("- ").append(tool.getName())
                  .append(": ").append(tool.getDescription()).append("\n");
        }
        prompt.append("\n");

        // Add user input or context from state
        String userInput = state.get("userInput");
        if (userInput != null) {
            prompt.append("User request: ").append(userInput).append("\n");
        }

        // Add previous tool calls if any
        List<AgentState.ToolCall> previousCalls = state.getToolCalls();
        if (!previousCalls.isEmpty()) {
            prompt.append("\nPrevious tool calls:\n");
            for (AgentState.ToolCall call : previousCalls) {
                prompt.append("- ").append(call.getToolName())
                      .append(": ").append(call.getResult()).append("\n");
            }
        }

        return prompt.toString();
    }

    /**
     * Builder for AutoNode
     */
    public static <S extends AgentState> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends AgentState> {
        private ToolCallingLLM llm;
        private List<Tool> tools = new ArrayList<>();
        private int maxIterations = 5;
        private String systemPrompt = "";

        public Builder<S> llm(ToolCallingLLM llm) {
            this.llm = llm;
            return this;
        }

        public Builder<S> addTool(Tool tool) {
            this.tools.add(tool);
            return this;
        }

        public Builder<S> tools(List<Tool> tools) {
            this.tools = new ArrayList<>(tools);
            return this;
        }

        public Builder<S> maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder<S> systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public AutoNode<S> build() {
            if (llm == null) {
                throw new IllegalStateException("LLM is required");
            }
            if (tools.isEmpty()) {
                throw new IllegalStateException("At least one tool is required");
            }
            return new AutoNode<>(this);
        }
    }
}
