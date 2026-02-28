package io.github.fal1winter.langgraph4j.persistence;

import io.github.fal1winter.langgraph4j.core.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {

    static class TestState extends State {
        public TestState(String value) {
            put("value", value);
        }

        public String getValue() {
            return get("value");
        }
    }

    @Test
    void testInMemoryStateStore() throws Exception {
        InMemoryStateStore<TestState> store = new InMemoryStateStore<>();

        TestState state = new TestState("test");
        store.save("checkpoint1", state);

        assertTrue(store.exists("checkpoint1"));
        assertFalse(store.exists("checkpoint2"));

        TestState loaded = store.load("checkpoint1");
        assertEquals("test", loaded.getValue());

        List<String> checkpoints = store.listCheckpoints();
        assertEquals(1, checkpoints.size());
        assertTrue(checkpoints.contains("checkpoint1"));

        store.delete("checkpoint1");
        assertFalse(store.exists("checkpoint1"));
    }

    @Test
    void testFileStateStore(@TempDir Path tempDir) throws Exception {
        FileStateStore<TestState> store = new FileStateStore<>(tempDir.toString());

        TestState state = new TestState("test");
        store.save("checkpoint1", state);

        assertTrue(store.exists("checkpoint1"));

        TestState loaded = store.load("checkpoint1");
        assertEquals("test", loaded.getValue());

        List<String> checkpoints = store.listCheckpoints();
        assertEquals(1, checkpoints.size());

        store.delete("checkpoint1");
        assertFalse(store.exists("checkpoint1"));
    }

    @Test
    void testLoadNonExistentCheckpoint() {
        InMemoryStateStore<TestState> store = new InMemoryStateStore<>();

        assertThrows(Exception.class, () -> store.load("nonexistent"));
    }
}
