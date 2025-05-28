package studio.goodlabs;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class StreamTableJoinApp {
    private static final String ORDER_TOPIC = "order-topic";
    private static final String ADDRESS_TOPIC = "address-topic";
    private static final String OUTPUT_TOPIC = "stream-table-output-topic";

    private static final String BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS");

    public static void main(String[] args) {
        Properties props = DemoStreamsUtils
                .getStreamsConfig("stream-table-join-app", BOOTSTRAP_SERVERS);

        DemoStreamsUtils.waitForTopics(
                props,
                List.of(ORDER_TOPIC, ADDRESS_TOPIC, OUTPUT_TOPIC)
        );

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> orders = builder.stream(
                ORDER_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );
        KTable<String, String> addresses = builder.table(
                ADDRESS_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        KStream<String, String> joined = orders.join(
                addresses,
                (order, addr) -> "{\"order\":" + order + ",\"address\":" + addr + "}"
        );

        joined.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }
}
