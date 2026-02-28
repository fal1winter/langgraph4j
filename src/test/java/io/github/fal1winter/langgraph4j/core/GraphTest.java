package io.github.fal1winter.langgraph4j.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GraphTest {

    static class TestState extends State {
        public TestState(String input) {
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

    @Test
    void testSimpleLinearWorkflow() throws Exception {
        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("step1", state -> {
                state.put("step1", "done");
                return state;
            })
            .addNode("step2", state -> {
                state.put("step2", "done");
                return state;
            })
            .addNode("step3", state -> {
                state.setOutput("completed");
                return state;
            })
            .setEntryPoint("step1")
            .addEdge("step1", "step2")
            .addEdge("step2", "step3")
            .addEdge("step3", Graph.END);

        TestState state = new TestState("test");
        TestState result = graph.execute(state);

        assertEquals("done", result.get("step1"));
        assertEquals("done", result.get("step2"));
        assertEquals("completed", result.getOutput());
    }

    @Test
    void testConditionalBranching() throws Exception {
        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("check", state -> {
                state.put("value", 10);
                return state;
            })
            .addNode("high", state -> {
                state.setOutput("high value");
                return state;
            })
            .addNode("low", state -> {
                state.setOutput("low value");
                return state;
            })
            .setEntryPoint("check")
            .addConditionalEdge("check", "high",
                state -> state.<Integer>get("value") > 5)
            .addConditionalEdge("check", "low",
                state -> state.<Integer>get("value") <= 5)
            .addEdge("high", Graph.END)
            .addEdge("low", Graph.END);

        TestState state = new TestState("test");
        TestState result = graph.execute(state);

        assertEquals("high value", result.getOutput());
    }

    @Test
    void testRouter() throws Exception {
        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("route", state -> {
                state.put("type", "A");
                return state;
            })
            .addNode("handleA", state -> {
                state.setOutput("handled A");
                return state;
            })
            .addNode("handleB", state -> {
                state.setOutput("handled B");
                return state;
            })
            .setEntryPoint("route")
            .addRouter("route", state -> {
                String type = state.get("type");
                return "A".equals(type) ? "handleA" : "handleB";
            })
            .addEdge("handleA", Graph.END)
            .addEdge("handleB", Graph.END);

        TestState state = new TestState("test");
        TestState result = graph.execute(state);

        assertEquals("handled A", result.getOutput());
    }

    @Test
    void testHumanInTheLoop() throws Exception {
        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("process", state -> {
                state.put("processed", true);
                return state;
            })
            .addNode("wait", state -> {
                state.setNeedsHumanInput(true);
                return state;
            })
            .setEntryPoint("process")
            .addEdge("process", "wait");

        TestState state = new TestState("test");
        TestState result = graph.execute(state);

        assertTrue(result.isNeedsHumanInput());
        assertTrue(result.get("processed"));
    }

    @Test
    void testErrorHandling() {
        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("fail", state -> {
                throw new RuntimeException("Test error");
            })
            .setEntryPoint("fail");

        TestState state = new TestState("test");

        assertThrows(Exception.class, () -> graph.execute(state));
    }

    @Test
    void testMaxIterations() throws Exception {
        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("loop", state -> {
                int count = state.get("count", 0);
                state.put("count", count + 1);
                return state;
            })
            .setEntryPoint("loop")
            .addEdge("loop", "loop") // Infinite loop
            .setMaxIterations(5);

        TestState state = new TestState("test");
        TestState result = graph.execute(state);

        assertTrue(result.hasError());
        assertTrue(result.getError().contains("maximum iterations"));
    }

    @Test
    void testGraphListener() throws Exception {
        final int[] nodeCount = {0};

        Graph<TestState> graph = Graph.<TestState>builder()
            .addNode("step1", state -> state)
            .addNode("step2", state -> state)
            .setEntryPoint("step1")
            .addEdge("step1", "step2")
            .addEdge("step2", Graph.END)
            .addListener(new GraphListener<TestState>() {
                @Override
                public void onAfterNode(String nodeName, TestState state) {
                    nodeCount[0]++;
                }
            });

        TestState state = new TestState("test");
        graph.execute(state);

        assertEquals(2, nodeCount[0]);
    }
}
