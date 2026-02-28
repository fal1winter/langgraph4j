package io.github.fal1winter.langgraph4j.examples;

import io.github.fal1winter.langgraph4j.core.Graph;
import io.github.fal1winter.langgraph4j.core.State;

/**
 * Simple example: A basic approval workflow
 * Demonstrates conditional branching and human-in-the-loop
 */
public class ApprovalWorkflowExample {

    static class ApprovalState extends State {
        public ApprovalState(String request) {
            put("request", request);
        }

        public String getRequest() {
            return get("request");
        }

        public void setApproved(boolean approved) {
            put("approved", approved);
        }

        public boolean isApproved() {
            return get("approved", false);
        }

        public void setResponse(String response) {
            put("response", response);
        }

        public String getResponse() {
            return get("response");
        }
    }

    public static void main(String[] args) throws Exception {
        // Build the workflow
        Graph<ApprovalState> workflow = Graph.<ApprovalState>builder()
            .addNode("validate", state -> {
                System.out.println("Validating request: " + state.getRequest());
                if (state.getRequest().length() < 10) {
                    state.setError("Request too short");
                    return state;
                }
                System.out.println("Validation passed");
                return state;
            })
            .addNode("wait_approval", state -> {
                System.out.println("Waiting for approval...");
                state.setNeedsHumanInput(true);
                return state;
            })
            .addNode("approve", state -> {
                System.out.println("Request approved!");
                state.setApproved(true);
                state.setResponse("Your request has been approved");
                return state;
            })
            .addNode("reject", state -> {
                System.out.println("Request rejected");
                state.setApproved(false);
                state.setResponse("Your request has been rejected");
                return state;
            })
            .setEntryPoint("validate")
            .addConditionalEdge("validate", "wait_approval",
                state -> !state.hasError())
            .addConditionalEdge("validate", Graph.END,
                State::hasError)
            .addEdge("wait_approval", "approve") // Simplified for demo
            .addEdge("approve", Graph.END)
            .addEdge("reject", Graph.END);

        // Execute workflow
        ApprovalState state = new ApprovalState("Please approve my vacation request");
        ApprovalState result = workflow.execute(state);

        if (result.isNeedsHumanInput()) {
            System.out.println("\n=== Workflow paused for human input ===");
            // In real scenario, wait for user input
            // Then resume with: result.setHumanInput("approved");
        } else {
            System.out.println("\nFinal result: " + result.getResponse());
        }
    }
}
