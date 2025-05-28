package studio.goodlabs;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;

public class DeduplicationTransformer implements Transformer<String, String, KeyValue<String, String>> {
    private final String storeName;
    private final long ttlMs;
    private KeyValueStore<String, Long> store;
    private ProcessorContext context;

    public DeduplicationTransformer(String storeName, long ttlMs) {
        this.storeName = storeName;
        this.ttlMs = ttlMs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        this.context = context;
        this.store = (KeyValueStore<String, Long>) context.getStateStore(storeName);
        // Schedule cleanup every 30 seconds
        this.context.schedule(Duration.ofSeconds(30), PunctuationType.WALL_CLOCK_TIME, this::cleanup);
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        long now = System.currentTimeMillis();
        Long lastSeen = store.get(key);
        if (lastSeen == null || now - lastSeen > ttlMs) {
            store.put(key, now);
            return KeyValue.pair(key, value);
        }

        return null;
    }

    private void cleanup(long timestamp) {
        try (KeyValueIterator<String, Long> iter = store.all()) {
            while (iter.hasNext()) {
                KeyValue<String, Long> entry = iter.next();
                if (timestamp - entry.value > ttlMs) {
                    store.delete(entry.key);
                }
            }
        }
    }

    @Override
    public void close() {
        // no need to handle
    }
}
