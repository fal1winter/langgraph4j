# Contributing to LangGraph4j

Thank you for your interest in contributing to LangGraph4j!

## How to Contribute

### Reporting Bugs

If you find a bug, please open an issue with:
- A clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Java version and environment details

### Suggesting Features

Feature requests are welcome! Please open an issue describing:
- The use case
- Why it would be useful
- Proposed API (if applicable)

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `mvn test`
6. Commit with clear messages
7. Push and open a PR

## Development Setup

```bash
# Clone the repository
git clone https://github.com/fal1winter/langgraph4j.git
cd langgraph4j

# Build the project
mvn clean install

# Run tests
mvn test

# Run examples
mvn exec:java -Dexec.mainClass="io.github.fal1winter.langgraph4j.examples.ApprovalWorkflowExample"
```

## Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Keep methods focused and concise

## Testing

- Write unit tests for new features
- Ensure existing tests pass
- Aim for high code coverage

## Questions?

Feel free to open an issue for any questions!
