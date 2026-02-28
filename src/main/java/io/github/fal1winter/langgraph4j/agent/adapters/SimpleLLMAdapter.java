package io.github.fal1winter.langgraph4j.agent.adapters;

import io.github.fal1winter.langgraph4j.agent.Tool;
import io.github.fal1winter.langgraph4j.agent.ToolCallingLLM;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple LLM adapter that uses text parsing for tool calls
 * Works with any LLM that can generate structured text
 *
 * Expected format from LLM:
 * TOOL_CALL: toolName(param1, param2)
 * or
 * FINISH: final response text
 */
public class SimpleLLMAdapter implements ToolCallingLLM {

    private final LLMFunction llmFunction;
    private static final Pattern TOOL_CALL_PATTERN =
        Pattern.compile("TOOL_CALL:\\s*(\\w+)\\(([^)]*)\\)");

    /**
     * Functional interface for LLM invocation
     */
    @FunctionalInterface
    public interface LLMFunction {
        String generate(String prompt);
    }

    public SimpleLLMAdapter(LLMFunction llmFunction) {
        this.llmFunction = llmFunction;
    }

    @Override
    public LLMResponse generate(String prompt, List<Tool> availableTools) {
        // Call the LLM
        String response = llmFunction.generate(prompt);

        // Parse response for tool calls
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        boolean finished = true;

        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String params = matcher.group(2);
            toolCalls.add(new ToolCallRequest(toolName, params));
            finished = false;
        }

        // Check for FINISH marker
        if (response.contains("FINISH:")) {
            finished = true;
            response = response.substring(response.indexOf("FINISH:") + 7).trim();
        }

        return new LLMResponse(response, toolCalls, finished);
    }

    /**
     * Create adapter from any LLM that implements the function interface
     */
    public static SimpleLLMAdapter fromFunction(LLMFunction function) {
        return new SimpleLLMAdapter(function);
    }
}
