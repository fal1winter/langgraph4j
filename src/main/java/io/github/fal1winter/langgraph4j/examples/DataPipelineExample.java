package io.github.fal1winter.langgraph4j.examples;

import io.github.fal1winter.langgraph4j.core.Graph;
import io.github.fal1winter.langgraph4j.core.State;

/**
 * Advanced example: Multi-step data processing pipeline
 * Demonstrates dynamic routing, loops, and error handling
 */
public class DataPipelineExample {

    static class PipelineState extends State {
        public PipelineState(String rawData) {
            put("rawData", rawData);
            put("retryCount", 0);
        }

        public String getRawData() {
            return get("rawData");
        }

        public void setCleanedData(String data) {
            put("cleanedData", data);
        }

        public String getCleanedData() {
            return get("cleanedData");
        }

        public void setValidationResult(boolean valid) {
            put("valid", valid);
        }

        public boolean isValid() {
            return get("valid", false);
        }

        public void incrementRetry() {
            int count = get("retryCount", 0);
            put("retryCount", count + 1);
        }

        public int getRetryCount() {
            return get("retryCount", 0);
        }

        public void setResult(String result) {
            put("result", result);
        }

        public String getResult() {
            return get("result");
        }
    }

    public static void main(String[] args) throws Exception {
        Graph<PipelineState> pipeline = Graph.<PipelineState>builder()
            // Node 1: Clean data
            .addNode("clean", state -> {
                System.out.println("Cleaning data...");
                String cleaned = state.getRawData().trim().toLowerCase();
                state.setCleanedData(cleaned);
                return state;
            })
            // Node 2: Validate data
            .addNode("validate", state -> {
                System.out.println("Validating data...");
                String data = state.getCleanedData();
                boolean valid = data.length() > 5 && !data.contains("error");
                state.setValidationResult(valid);
                return state;
            })
            // Node 3: Retry cleaning with different strategy
            .addNode("retry_clean", state -> {
                System.out.println("Retrying with enhanced cleaning...");
                state.incrementRetry();
                String enhanced = state.getRawData().replaceAll("[^a-zA-Z0-9]", "");
                state.setCleanedData(enhanced);
                return state;
            })
            // Node 4: Process valid data
            .addNode("process", state -> {
                System.out.println("Processing valid data...");
                String result = "Processed: " + state.getCleanedData().toUpperCase();
                state.setResult(result);
                return state;
            })
            // Node 5: Handle invalid data
            .addNode("handle_invalid", state -> {
                System.out.println("Handling invalid data...");
                state.setResult("Data validation failed after " + state.getRetryCount() + " retries");
                return state;
            })
            .setEntryPoint("clean")
            .addEdge("clean", "validate")
            // Dynamic routing based on validation
            .addRouter("validate", state -> {
                if (state.isValid()) {
                    return "process";
                } else if (state.getRetryCount() < 2) {
                    return "retry_clean"; // Loop back
                } else {
                    return "handle_invalid";
                }
            })
            .addEdge("retry_clean", "validate") // Loop
            .addEdge("process", Graph.END)
            .addEdge("handle_invalid", Graph.END)
            .setMaxIterations(10);

        // Test case 1: Valid data
        System.out.println("=== Test 1: Valid Data ===");
        PipelineState state1 = new PipelineState("  Hello World  ");
        PipelineState result1 = pipeline.execute(state1);
        System.out.println("Result: " + result1.getResult());

        // Test case 2: Invalid data with retry
        System.out.println("\n=== Test 2: Invalid Data (will retry) ===");
        PipelineState state2 = new PipelineState("err");
        PipelineState result2 = pipeline.execute(state2);
        System.out.println("Result: " + result2.getResult());
    }
}
