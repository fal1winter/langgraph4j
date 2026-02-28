package io.github.fal1winter.langgraph4j.agent;

import io.github.fal1winter.langgraph4j.core.State;

import java.util.ArrayList;
import java.util.List;

/**
 * State extension for agent nodes with tool calling
 */
public class AgentState extends State {

    /**
     * Add a tool call record
     */
    public void addToolCall(String toolName, Object parameters, String result) {
        List<ToolCall> calls = get("toolCalls", new ArrayList<>());
        calls.add(new ToolCall(toolName, parameters, result));
        put("toolCalls", calls);
    }

    /**
     * Get all tool calls
     */
    public List<ToolCall> getToolCalls() {
        return get("toolCalls", new ArrayList<>());
    }

    /**
     * Get the last tool call result
     */
    public String getLastToolResult() {
        List<ToolCall> calls = getToolCalls();
        return calls.isEmpty() ? null : calls.get(calls.size() - 1).getResult();
    }

    /**
     * Set LLM response
     */
    public void setLLMResponse(String response) {
        put("llmResponse", response);
    }

    /**
     * Get LLM response
     */
    public String getLLMResponse() {
        return get("llmResponse");
    }

    /**
     * Check if should continue (more tools to call)
     */
    public boolean shouldContinue() {
        return get("shouldContinue", false);
    }

    /**
     * Set should continue flag
     */
    public void setShouldContinue(boolean shouldContinue) {
        put("shouldContinue", shouldContinue);
    }

    /**
     * Tool call record
     */
    public static class ToolCall {
        private final String toolName;
        private final Object parameters;
        private final String result;
        private final long timestamp;

        public ToolCall(String toolName, Object parameters, String result) {
            this.toolName = toolName;
            this.parameters = parameters;
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        public String getToolName() {
            return toolName;
        }

        public Object getParameters() {
            return parameters;
        }

        public String getResult() {
            return result;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "ToolCall{" +
                    "toolName='" + toolName + '\'' +
                    ", parameters=" + parameters +
                    ", result='" + result + '\'' +
                    '}';
        }
    }
}
