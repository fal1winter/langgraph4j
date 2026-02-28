package io.github.fal1winter.langgraph4j.agent;

/**
 * Interface for LLM that can call tools
 * Abstracts away the specific LLM implementation
 */
public interface ToolCallingLLM {

    /**
     * Generate response with tool calling capability
     *
     * @param prompt the user prompt
     * @param availableTools tools that can be called
     * @return LLM response with tool calls
     */
    LLMResponse generate(String prompt, java.util.List<Tool> availableTools);

    /**
     * LLM response containing text and optional tool calls
     */
    class LLMResponse {
        private final String text;
        private final java.util.List<ToolCallRequest> toolCalls;
        private final boolean finished;

        public LLMResponse(String text, java.util.List<ToolCallRequest> toolCalls, boolean finished) {
            this.text = text;
            this.toolCalls = toolCalls != null ? toolCalls : new java.util.ArrayList<>();
            this.finished = finished;
        }

        public String getText() {
            return text;
        }

        public java.util.List<ToolCallRequest> getToolCalls() {
            return toolCalls;
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean hasToolCalls() {
            return !toolCalls.isEmpty();
        }
    }

    /**
     * Tool call request from LLM
     */
    class ToolCallRequest {
        private final String toolName;
        private final Object parameters;

        public ToolCallRequest(String toolName, Object parameters) {
            this.toolName = toolName;
            this.parameters = parameters;
        }

        public String getToolName() {
            return toolName;
        }

        public Object getParameters() {
            return parameters;
        }
    }
}
