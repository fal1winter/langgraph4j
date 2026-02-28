package io.github.fal1winter.langgraph4j.agent.adapters;

import io.github.fal1winter.langgraph4j.agent.Tool;
import io.github.fal1winter.langgraph4j.agent.ToolCallingLLM;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for LangChain4j tools
 * Allows using LangChain4j @Tool annotated methods with LangGraph4j AutoNode
 */
public class LangChain4jToolAdapter implements Tool {

    private final Object toolInstance;
    private final Method method;
    private final String name;
    private final String description;

    public LangChain4jToolAdapter(Object toolInstance, Method method) {
        this.toolInstance = toolInstance;
        this.method = method;
        this.method.setAccessible(true);

        // Extract name and description from @Tool annotation
        dev.langchain4j.agent.tool.Tool annotation =
            method.getAnnotation(dev.langchain4j.agent.tool.Tool.class);

        if (annotation != null) {
            this.name = annotation.name().isEmpty() ? method.getName() : annotation.name();
            String[] values = annotation.value();
            this.description = values.length > 0 ? values[0] : "No description";
        } else {
            this.name = method.getName();
            this.description = "No description";
        }
    }

    @Override
    public String execute(Object parameters) throws Exception {
        // Convert parameters to method arguments
        Object[] args = convertParameters(parameters);

        // Invoke the method
        Object result = method.invoke(toolInstance, args);

        return result != null ? result.toString() : "";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Convert parameters to method arguments
     * Supports simple parameter passing for now
     */
    private Object[] convertParameters(Object parameters) {
        if (parameters == null) {
            return new Object[0];
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return new Object[0];
        }

        // Simple case: single parameter
        if (paramTypes.length == 1) {
            return new Object[]{convertParameter(parameters, paramTypes[0])};
        }

        // Multiple parameters: expect Map or array
        if (parameters instanceof java.util.Map) {
            return convertFromMap((java.util.Map<?, ?>) parameters, paramTypes);
        } else if (parameters instanceof Object[]) {
            return (Object[]) parameters;
        }

        // Fallback: wrap in array
        return new Object[]{parameters};
    }

    private Object convertParameter(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // Direct assignment if compatible
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // String to primitive conversions
        if (value instanceof String) {
            String str = (String) value;
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(str);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(str);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(str);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(str);
            }
        }

        return value;
    }

    private Object[] convertFromMap(java.util.Map<?, ?> map, Class<?>[] paramTypes) {
        Object[] args = new Object[paramTypes.length];

        // Try to match by parameter names (requires -parameters compiler flag)
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Object value = map.get(paramName);
            args[i] = convertParameter(value, paramTypes[i]);
        }

        return args;
    }

    /**
     * Scan an object for @Tool annotated methods and create adapters
     */
    public static List<Tool> fromToolsObject(Object toolsObject) {
        List<Tool> tools = new ArrayList<>();

        for (Method method : toolsObject.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class)) {
                tools.add(new LangChain4jToolAdapter(toolsObject, method));
            }
        }

        return tools;
    }
}
