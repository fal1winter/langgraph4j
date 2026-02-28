package io.github.fal1winter.langgraph4j.agent;

import java.util.function.Predicate;

/**
 * Tool execution policy for controlling tool access and invocation
 */
public class ToolExecutionPolicy {

    private final Predicate<String> toolFilter;
    private final Predicate<ToolCallContext> beforeExecute;
    private final Predicate<ToolCallContext> afterExecute;
    private final int maxToolCallsPerIteration;
    private final int maxTotalToolCalls;

    private ToolExecutionPolicy(Builder builder) {
        this.toolFilter = builder.toolFilter;
        this.beforeExecute = builder.beforeExecute;
        this.afterExecute = builder.afterExecute;
        this.maxToolCallsPerIteration = builder.maxToolCallsPerIteration;
        this.maxTotalToolCalls = builder.maxTotalToolCalls;
    }

    /**
     * Check if a tool is allowed to be called
     */
    public boolean isToolAllowed(String toolName) {
        return toolFilter == null || toolFilter.test(toolName);
    }

    /**
     * Check before tool execution
     * @return true to allow execution, false to skip
     */
    public boolean beforeToolExecution(ToolCallContext context) {
        return beforeExecute == null || beforeExecute.test(context);
    }

    /**
     * Check after tool execution
     * @return true to continue, false to stop
     */
    public boolean afterToolExecution(ToolCallContext context) {
        return afterExecute == null || afterExecute.test(context);
    }

    public int getMaxToolCallsPerIteration() {
        return maxToolCallsPerIteration;
    }

    public int getMaxTotalToolCalls() {
        return maxTotalToolCalls;
    }

    /**
     * Tool call context for policy checks
     */
    public static class ToolCallContext {
        private final String toolName;
        private final Object parameters;
        private final int iterationNumber;
        private final int totalToolCallsSoFar;
        private String result;
        private Exception error;

        public ToolCallContext(String toolName, Object parameters,
                              int iterationNumber, int totalToolCallsSoFar) {
            this.toolName = toolName;
            this.parameters = parameters;
            this.iterationNumber = iterationNumber;
            this.totalToolCallsSoFar = totalToolCallsSoFar;
        }

        public String getToolName() { return toolName; }
        public Object getParameters() { return parameters; }
        public int getIterationNumber() { return iterationNumber; }
        public int getTotalToolCallsSoFar() { return totalToolCallsSoFar; }
        public String getResult() { return result; }
        public Exception getError() { return error; }

        public void setResult(String result) { this.result = result; }
        public void setError(Exception error) { this.error = error; }
        public boolean hasError() { return error != null; }
    }

    /**
     * Builder for ToolExecutionPolicy
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Predicate<String> toolFilter;
        private Predicate<ToolCallContext> beforeExecute;
        private Predicate<ToolCallContext> afterExecute;
        private int maxToolCallsPerIteration = Integer.MAX_VALUE;
        private int maxTotalToolCalls = Integer.MAX_VALUE;

        /**
         * Set tool whitelist
         */
        public Builder allowTools(String... toolNames) {
            java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(toolNames));
            this.toolFilter = allowed::contains;
            return this;
        }

        /**
         * Set tool blacklist
         */
        public Builder denyTools(String... toolNames) {
            java.util.Set<String> denied = new java.util.HashSet<>(java.util.Arrays.asList(toolNames));
            this.toolFilter = name -> !denied.contains(name);
            return this;
        }

        /**
         * Custom tool filter
         */
        public Builder toolFilter(Predicate<String> filter) {
            this.toolFilter = filter;
            return this;
        }

        /**
         * Hook before tool execution
         */
        public Builder beforeExecute(Predicate<ToolCallContext> check) {
            this.beforeExecute = check;
            return this;
        }

        /**
         * Hook after tool execution
         */
        public Builder afterExecute(Predicate<ToolCallContext> check) {
            this.afterExecute = check;
            return this;
        }

        /**
         * Maximum tool calls per iteration
         */
        public Builder maxToolCallsPerIteration(int max) {
            this.maxToolCallsPerIteration = max;
            return this;
        }

        /**
         * Maximum total tool calls across all iterations
         */
        public Builder maxTotalToolCalls(int max) {
            this.maxTotalToolCalls = max;
            return this;
        }

        public ToolExecutionPolicy build() {
            return new ToolExecutionPolicy(this);
        }
    }

    /**
     * Default policy - allow all
     */
    public static ToolExecutionPolicy allowAll() {
        return builder().build();
    }

    /**
     * Restrictive policy - whitelist only
     */
    public static ToolExecutionPolicy whitelist(String... toolNames) {
        return builder().allowTools(toolNames).build();
    }
}
