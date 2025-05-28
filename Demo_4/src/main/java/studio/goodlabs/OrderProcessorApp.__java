package studio.goodlabs;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.List;
import java.util.Properties;

public class OrderProcessorApp {
    private static final String RAW_ORDER_TOPIC = "raw-order-topic";
    private static final String CLEAN_ORDER_TOPIC = "clean-order-topic";

    private static final String BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS");

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-processor");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        DemoStreamsUtils.waitForTopics(
                props,
                List.of(RAW_ORDER_TOPIC, CLEAN_ORDER_TOPIC)
        );

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> orders = builder.stream(RAW_ORDER_TOPIC);
        orders.to(CLEAN_ORDER_TOPIC);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
