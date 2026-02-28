package io.github.fal1winter.langgraph4j.agent;

/**
 * Tool interface for auto nodes
 * Similar to LangChain4j's @Tool annotation
 */
@FunctionalInterface
public interface Tool {

    /**
     * Execute the tool
     *
     * @param parameters tool parameters as JSON string or map
     * @return tool execution result
     * @throws Exception if execution fails
     */
    String execute(Object parameters) throws Exception;

    /**
     * Get tool name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Get tool description
     */
    default String getDescription() {
        return "No description provided";
    }
}
