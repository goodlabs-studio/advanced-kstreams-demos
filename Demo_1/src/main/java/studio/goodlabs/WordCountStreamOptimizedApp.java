package studio.goodlabs;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.Options;
import org.rocksdb.CompactionStyle;
import org.rocksdb.RocksDB;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class WordCountStreamOptimizedApp {
    private static final String BOOTSTRAP_SERVERS = System.getenv("BOOTSTRAP_SERVERS");

    public static class OptimizedRocksDBConfig implements RocksDBConfigSetter {
        static { RocksDB.loadLibrary(); }

        @Override
        public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {
            // Lower space amplification
            options.setCompactionStyle(CompactionStyle.LEVEL);
            // Prevent OOM: smaller memtables
            options.setWriteBufferSize(8 * 1024 * 1024);       // 8 MB
            options.setMaxWriteBufferNumber(2);                // 2 buffers
            // Limit threads to reduce I/O/CPU contention
            options.setMaxBackgroundCompactions(1);            // 1 compaction thread
            options.setMaxBackgroundFlushes(1);                // 1 flush thread
        }

        @Override
        public void close(final String storeName, final Options options) { /* no-op */ }
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-optimized-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 1); // limit cores :contentReference[oaicite:14]{index=14}
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        // Hook in our RocksDBConfigSetter
        props.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG,
                OptimizedRocksDBConfig.class.getName());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream(
                "input-topic",
                Consumed.with(Serdes.String(), Serdes.String())
        );

        KTable<String, Long> wordCounts = textLines
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"));

        wordCounts
                .toStream()
                .to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
