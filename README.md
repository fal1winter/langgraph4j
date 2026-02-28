# LangGraph4j

[![Maven Central](https://img.shields.io/maven-central/v/io.github.fal1winter/langgraph4j.svg)](https://search.maven.org/artifact/io.github.fal1winter/langgraph4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://www.oracle.com/java/)

A Java implementation of [LangGraph](https://github.com/langchain-ai/langgraph) for building **stateful, multi-step AI agent workflows** with graph-based orchestration.

## Features

- 🔄 **Graph-based Workflow**: Define workflows as directed graphs with nodes and edges
- 🎯 **Dynamic Routing**: Conditional branching and multi-way routing based on state
- 🔁 **Loop Support**: Build iterative workflows with cycle detection
- 👤 **Human-in-the-Loop**: Pause execution for human input and resume seamlessly
- 💾 **State Persistence**: Built-in checkpointing with file and in-memory stores
- 🎧 **Event Listeners**: Monitor workflow execution with lifecycle hooks
- 🔍 **Visualization**: Generate Mermaid and DOT diagrams for workflows
- 🧪 **Type-Safe**: Strongly typed with Java generics
- 🪶 **Lightweight**: Minimal dependencies (only SLF4J)

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.fal1winter</groupId>
    <artifactId>langgraph4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.fal1winter:langgraph4j:1.0.0'
```

## Quick Start

### 1. Define Your State

```java
import io.github.fal1winter.langgraph4j.core.State;

public class MyState extends State {
    public void setInput(String input) {
        put("input", input);
    }

    public String getInput() {
        return get("input");
    }

    public void setOutput(String output) {
        put("output", output);
    }

    public String getOutput() {
        return get("output");
    }
}
```

### 2. Build Your Workflow

```java
import io.github.fal1winter.langgraph4j.core.Graph;

Graph<MyState> workflow = Graph.<MyState>builder()
    // Add nodes
    .addNode("process", state -> {
        String input = state.getInput();
        state.setOutput("Processed: " + input);
        return state;
    })
    .addNode("validate", state -> {
        // Validation logic
        return state;
    })

    // Define flow
    .setEntryPoint("process")
    .addEdge("process", "validate")
    .addEdge("validate", Graph.END);
```

### 3. Execute

```java
MyState initialState = new MyState();
initialState.setInput("Hello World");

MyState result = workflow.execute(initialState);
System.out.println(result.getOutput()); // "Processed: Hello World"
```

## Core Concepts

### Nodes

Nodes are units of work that transform state:

```java
.addNode("my_node", state -> {
    // Your logic here
    state.put("key", "value");
    return state;
})
```

### Edges

**Unconditional edges** always transition:

```java
.addEdge("node1", "node2")
```

**Conditional edges** transition based on state:

```java
.addConditionalEdge("node1", "node2",
    state -> state.get("score") > 5)
```

### Routers

Routers enable multi-way branching:

```java
.addRouter("decision_node", state -> {
    switch (state.get("type")) {
        case "A": return "handle_a";
        case "B": return "handle_b";
        default: return Graph.END;
    }
})
```

### Human-in-the-Loop

Pause execution for human input:

```java
.addNode("wait_approval", state -> {
    state.setNeedsHumanInput(true);
    return state;
})

// Execute
MyState result = workflow.execute(initialState);

if (result.isNeedsHumanInput()) {
    // Wait for user input
    result.setHumanInput("approved");

    // Resume execution
    result = workflow.execute(result);
}
```

## Advanced Features

### State Persistence

```java
import io.github.fal1winter.langgraph4j.persistence.InMemoryStateStore;

InMemoryStateStore<MyState> store = new InMemoryStateStore<>();

// Save checkpoint
store.save("checkpoint1", state);

// Load checkpoint
MyState restored = store.load("checkpoint1");
```

### Event Listeners

```java
import io.github.fal1winter.langgraph4j.core.GraphListener;

workflow.addListener(new GraphListener<MyState>() {
    @Override
    public void onBeforeNode(String nodeName, MyState state) {
        System.out.println("Executing: " + nodeName);
    }

    @Override
    public void onAfterNode(String nodeName, MyState state) {
        System.out.println("Completed: " + nodeName);
    }
});
```

### Loop Detection

```java
workflow.setMaxIterations(100); // Prevent infinite loops
```

### Visualization

```java
import io.github.fal1winter.langgraph4j.utils.GraphVisualizer;

// Generate Mermaid diagram
String mermaid = GraphVisualizer.toMermaid(workflow);

// Generate DOT format for Graphviz
String dot = GraphVisualizer.toDot(workflow);
```

## Examples

### Approval Workflow

```java
Graph<ApprovalState> workflow = Graph.<ApprovalState>builder()
    .addNode("validate", state -> {
        if (state.getRequest().length() < 10) {
            state.setError("Request too short");
        }
        return state;
    })
    .addNode("approve", state -> {
        state.setApproved(true);
        return state;
    })
    .addNode("reject", state -> {
        state.setApproved(false);
        return state;
    })
    .setEntryPoint("validate")
    .addConditionalEdge("validate", "approve",
        state -> !state.hasError())
    .addConditionalEdge("validate", "reject",
        State::hasError)
    .addEdge("approve", Graph.END)
    .addEdge("reject", Graph.END);
```

### AI Agent with Dynamic Routing

```java
Graph<AgentState> agent = Graph.<AgentState>builder()
    .addNode("analyze_intent", state -> {
        // Analyze user query
        String intent = detectIntent(state.getQuery());
        state.setIntent(intent);
        return state;
    })
    .addNode("search_tool", state -> {
        // Execute search
        return state;
    })
    .addNode("calculate_tool", state -> {
        // Execute calculation
        return state;
    })
    .setEntryPoint("analyze_intent")
    .addRouter("analyze_intent", state -> {
        switch (state.getIntent()) {
            case "SEARCH": return "search_tool";
            case "CALCULATE": return "calculate_tool";
            default: return Graph.END;
        }
    })
    .addEdge("search_tool", Graph.END)
    .addEdge("calculate_tool", Graph.END);
```

### Data Pipeline with Retry Logic

```java
Graph<PipelineState> pipeline = Graph.<PipelineState>builder()
    .addNode("clean", state -> {
        state.setCleanedData(clean(state.getRawData()));
        return state;
    })
    .addNode("validate", state -> {
        boolean valid = validate(state.getCleanedData());
        state.setValid(valid);
        return state;
    })
    .addNode("retry", state -> {
        state.incrementRetry();
        // Enhanced cleaning
        return state;
    })
    .addNode("process", state -> {
        // Process valid data
        return state;
    })
    .setEntryPoint("clean")
    .addEdge("clean", "validate")
    .addRouter("validate", state -> {
        if (state.isValid()) {
            return "process";
        } else if (state.getRetryCount() < 3) {
            return "retry"; // Loop back
        } else {
            return Graph.END; // Give up
        }
    })
    .addEdge("retry", "validate") // Create loop
    .addEdge("process", Graph.END);
```

## Comparison with LangGraph (Python)

| Feature | LangGraph (Python) | LangGraph4j (Java) |
|---------|-------------------|-------------------|
| Graph Definition | ✅ | ✅ |
| Conditional Edges | ✅ | ✅ |
| Dynamic Routing | ✅ | ✅ |
| Human-in-the-Loop | ✅ | ✅ |
| State Persistence | ✅ | ✅ |
| Event Listeners | ✅ | ✅ |
| Visualization | ✅ | ✅ (Mermaid/DOT) |
| Type Safety | ❌ | ✅ |
| Async Execution | ✅ | 🔄 (Planned) |

## Use Cases

- **AI Agent Orchestration**: Build complex multi-step AI agents with tool calling
- **Business Process Automation**: Model approval workflows, data pipelines
- **State Machines**: Implement complex state machines with branching logic
- **ETL Pipelines**: Build data transformation workflows with retry logic
- **Chatbot Flows**: Create conversational flows with context management

## Requirements

- Java 11 or higher
- SLF4J 2.0+ (for logging)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by [LangGraph](https://github.com/langchain-ai/langgraph) from LangChain
- Built for the Java ecosystem

## Links

- [GitHub Repository](https://github.com/fal1winter/langgraph4j)
- [Issue Tracker](https://github.com/fal1winter/langgraph4j/issues)
- [LangGraph Documentation](https://langchain-ai.github.io/langgraph/)

## Support

If you find this project helpful, please consider giving it a ⭐️ on GitHub!
